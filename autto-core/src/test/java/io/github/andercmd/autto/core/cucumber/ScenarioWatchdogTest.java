package io.github.andercmd.autto.core.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ScenarioWatchdogTest {

    @Test
    void firesWhenTheScenarioRunsTooLong() throws Exception {
        CountDownLatch fired = new CountDownLatch(1);
        ScenarioWatchdog.Armed armed = ScenarioWatchdog.arm(Duration.ofMillis(50), fired::countDown);

        assertThat(fired.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(armed.disarm()).isTrue();
    }

    @Test
    void doesNotFireWhenDisarmedInTime() throws Exception {
        CountDownLatch fired = new CountDownLatch(1);
        ScenarioWatchdog.Armed armed = ScenarioWatchdog.arm(Duration.ofMillis(300), fired::countDown);

        assertThat(armed.disarm()).isFalse();
        assertThat(fired.await(600, TimeUnit.MILLISECONDS)).isFalse();
    }
}
