package io.github.andercmd.autto.core.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andercmd.autto.core.config.AuttoSettings;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseTest {

    private Database db;

    @BeforeEach
    void connect() {
        Properties system = new Properties();
        system.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
        system.setProperty("autto.db.url", "jdbc:h2:mem:autto-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        system.setProperty("autto.db.username", "sa");
        db = new Database(AuttoSettings.load(Map.of(), system));
        db.update("create table users(id int primary key, email varchar(100), status varchar(10))");
    }

    @AfterEach
    void close() {
        db.close();
    }

    @Test
    void insertsAndQueriesWithParameters() {
        assertThat(db.update("insert into users values (?, ?, ?)", 1, "a@example.test", "NEW")).isEqualTo(1);

        assertThat(db.query("select * from users where email = ?", "a@example.test"))
                .singleElement().satisfies(row -> assertThat(row).containsEntry("status", "NEW"));
        assertThat(db.scalar("select status from users where id = ?", 1)).contains("NEW");
        assertThat(db.scalar("select status from users where id = ?", 99)).isEmpty();
    }

    @Test
    void cleanupRunsInReverseOrderAndSurvivesFailures() {
        db.update("insert into users values (1, 'a', 'NEW')");
        db.cleanup("delete from users where id = ?", 1);
        db.cleanup("this is not sql");

        db.runCleanups();

        assertThat(db.scalar("select count(*) from users")).contains(0L);
    }

    @Test
    void sqlErrorsAreReportedWithTheStatement() {
        assertThatThrownBy(() -> db.query("select * from missing"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("select * from missing");
    }

    @Test
    void failsClearlyWithoutConfiguration() {
        Properties system = new Properties();
        system.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
        Database unconfigured = new Database(AuttoSettings.load(Map.of(), system));

        assertThatThrownBy(() -> unconfigured.query("select 1")).hasMessageContaining("autto.db.url");
    }
}
