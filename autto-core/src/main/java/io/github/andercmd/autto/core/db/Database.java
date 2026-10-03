package io.github.andercmd.autto.core.db;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.security.Secrets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain JDBC access to the database of the application under test, to prepare data and to verify results that the
 * UI does not show. Configure {@code autto.db.url|username|password} (the password is masked) and add the JDBC
 * driver of your database to the test project.
 *
 * <p>It is a scenario-scoped bean: statements registered with {@link #cleanup(String, Object...)} run in reverse
 * order when the scenario ends, even if it failed, so the data created by a scenario is always removed.
 *
 * <pre>{@code
 * db.update("insert into users(email) values (?)", email);
 * db.cleanup("delete from users where email = ?", email);
 * assertThat(db.scalar("select status from orders where ref = ?", ref)).contains("PAID");
 * }</pre>
 *
 * <p>Always use {@code ?} parameters; never concatenate values into the SQL text.
 */
public class Database implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);

    private final AuttoProperties.Db config;
    private final List<Cleanup> cleanups = new ArrayList<>();
    private Connection connection;

    public Database(AuttoSettings settings) {
        this.config = settings.properties().db();
        Secrets.register(config.password());
    }

    /** Runs a {@code select} and returns every row as a column-name to value map (column names lower-cased). */
    public List<Map<String, Object>> query(String sql, Object... params) {
        try (PreparedStatement statement = prepare(sql, params); ResultSet rows = statement.executeQuery()) {
            ResultSetMetaData meta = rows.getMetaData();
            List<Map<String, Object>> result = new ArrayList<>();
            while (rows.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnLabel(i).toLowerCase(java.util.Locale.ROOT), rows.getObject(i));
                }
                result.add(row);
            }
            return result;
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    /** First column of the first row, empty when the query returns nothing. */
    public Optional<Object> scalar(String sql, Object... params) {
        return query(sql, params).stream().findFirst().map(row -> row.values().iterator().next());
    }

    /** Runs an {@code insert}, {@code update} or {@code delete}; returns the number of affected rows. */
    public int update(String sql, Object... params) {
        try (PreparedStatement statement = prepare(sql, params)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    /** Registers a statement that runs when the scenario ends (last registered runs first). */
    public void cleanup(String sql, Object... params) {
        cleanups.add(new Cleanup(sql, params));
    }

    /** Runs the registered cleanup statements; failures are logged and never mask the scenario result. */
    public void runCleanups() {
        for (int i = cleanups.size() - 1; i >= 0; i--) {
            Cleanup cleanup = cleanups.get(i);
            try {
                update(cleanup.sql(), cleanup.params());
            } catch (RuntimeException e) {
                LOG.warn("Cleanup failed ({}): {}", cleanup.sql(), e.getMessage());
            }
        }
        cleanups.clear();
    }

    @Override
    public void close() {
        runCleanups();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.debug("Connection did not close cleanly: {}", e.getMessage());
            }
            connection = null;
        }
    }

    private PreparedStatement prepare(String sql, Object[] params) throws SQLException {
        PreparedStatement statement = connection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
        return statement;
    }

    private Connection connection() throws SQLException {
        if (config.url() == null) {
            throw new IllegalStateException("autto.db.url is not configured (application.yml or AUTTO_DB_URL)");
        }
        if (connection == null || connection.isClosed()) {
            if (config.driverClass() != null) {
                try {
                    Class.forName(config.driverClass());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("JDBC driver " + config.driverClass()
                            + " is not on the classpath. Add it to the test project dependencies", e);
                }
            }
            connection = java.sql.DriverManager.getConnection(config.url(), config.username(), config.password());
            connection.setAutoCommit(true);
        }
        return connection;
    }

    private static IllegalStateException failure(String sql, SQLException e) {
        return new IllegalStateException("Database statement failed: " + sql + " → " + e.getMessage(), e);
    }

    private record Cleanup(String sql, Object[] params) {
    }
}
