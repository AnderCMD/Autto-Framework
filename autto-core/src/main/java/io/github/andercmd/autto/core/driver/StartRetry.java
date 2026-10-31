package io.github.andercmd.autto.core.driver;

import io.github.andercmd.autto.core.observability.RunMetrics;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries the creation of a browser session with exponential back-off. Remote infrastructure (Selenium Grid, cloud
 * providers, Docker) is often busy for a few seconds; a second attempt avoids failing the whole scenario.
 *
 * <p>Configuration errors ({@link IllegalArgumentException}, {@link IllegalStateException}) are never retried.
 */
final class StartRetry {

    private static final Logger LOG = LoggerFactory.getLogger(StartRetry.class);

    private StartRetry() {
    }

    static <T> T call(int retries, Duration delay, Supplier<T> action) {
        Duration pause = delay;
        for (int attempt = 0; ; attempt++) {
            try {
                return action.get();
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw e;
            } catch (RuntimeException e) {
                if (attempt >= retries) {
                    throw e;
                }
                RunMetrics.global().browserStartRetried();
                LOG.warn("Browser session could not be created (attempt {} of {}): {}. Retrying in {} ms",
                        attempt + 1, retries + 1, firstLine(e), pause.toMillis());
                sleep(pause);
                pause = pause.multipliedBy(2);
            }
        }
    }

    private static void sleep(Duration pause) {
        if (pause.isZero()) {
            return;
        }
        try {
            Thread.sleep(pause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry the browser start", e);
        }
    }

    private static String firstLine(RuntimeException e) {
        String message = String.valueOf(e.getMessage());
        int newLine = message.indexOf('\n');
        return newLine > 0 ? message.substring(0, newLine) : message;
    }
}
