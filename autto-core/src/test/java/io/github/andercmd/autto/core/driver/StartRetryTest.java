package io.github.andercmd.autto.core.driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.SessionNotCreatedException;

class StartRetryTest {

    @Test
    void retriesTransientFailures() {
        AtomicInteger attempts = new AtomicInteger();
        String result = StartRetry.call(2, Duration.ZERO, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new SessionNotCreatedException("grid busy");
            }
            return "session";
        });
        assertThat(result).isEqualTo("session");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void givesUpAfterTheLastAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> StartRetry.call(1, Duration.ZERO, () -> {
            attempts.incrementAndGet();
            throw new SessionNotCreatedException("grid down");
        })).isInstanceOf(SessionNotCreatedException.class);
        assertThat(attempts).hasValue(2);
    }

    @Test
    void configurationErrorsAreNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> StartRetry.call(3, Duration.ZERO, () -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("Safari cannot run in Docker");
        })).isInstanceOf(IllegalArgumentException.class);
        assertThat(attempts).hasValue(1);
    }
}
