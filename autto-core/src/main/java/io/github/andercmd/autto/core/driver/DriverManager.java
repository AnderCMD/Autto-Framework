package io.github.andercmd.autto.core.driver;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import java.util.Optional;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe holder of the current {@link DriverSession}.
 *
 * <p>Every thread (that is, every scenario running in parallel) owns its own browser. Page objects call
 * {@link #driver()} instead of keeping a reference, which keeps them stateless and parallel friendly.
 */
public final class DriverManager {

    private static final Logger LOG = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<DriverSession> SESSION = new ThreadLocal<>();

    private DriverManager() {
    }

    /** Starts a new browser for the current thread using the global configuration. */
    public static DriverSession start() {
        return start(AuttoSettings.get());
    }

    /**
     * Starts a new browser for the current thread. Any previous browser of this thread is closed first. Failed
     * starts are retried {@code autto.driver.start-retries} times.
     */
    public static DriverSession start(AuttoSettings settings) {
        quit();
        AuttoProperties.Driver driver = settings.properties().driver();
        DriverHandle handle = StartRetry.call(driver.startRetries(), driver.startRetryDelay(),
                () -> DriverFactory.create(settings));
        DriverSession session = new DriverSession(handle, settings.properties());
        SESSION.set(session);
        LOG.info("Session started: {} ({})", session.details().label(), session.description());
        return session;
    }

    /** The browser of the current thread. */
    public static WebDriver driver() {
        return session().driver();
    }

    public static DriverSession session() {
        DriverSession session = SESSION.get();
        if (session == null) {
            throw new IllegalStateException("No browser is running on thread '" + Thread.currentThread().getName()
                    + "'. Browsers are started by BrowserHooks for scenarios not tagged with @nobrowser.");
        }
        return session;
    }

    public static Optional<DriverSession> currentSession() {
        return Optional.ofNullable(SESSION.get());
    }

    public static boolean isRunning() {
        return SESSION.get() != null;
    }

    /** Closes the browser of the current thread, if any. Never throws. */
    public static void quit() {
        DriverSession session = SESSION.get();
        if (session == null) {
            return;
        }
        SESSION.remove();
        try {
            session.quit();
        } catch (RuntimeException e) {
            LOG.warn("Browser did not close cleanly: {}", e.getMessage());
        }
    }
}
