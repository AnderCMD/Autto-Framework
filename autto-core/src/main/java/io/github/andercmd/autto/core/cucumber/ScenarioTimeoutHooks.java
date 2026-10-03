package io.github.andercmd.autto.core.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.report.Report;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aborts scenarios that exceed {@code autto.scenario.timeout}: the browser of the scenario is closed and the worker
 * thread is interrupted, so the scenario fails instead of blocking the whole (parallel) run.
 */
public class ScenarioTimeoutHooks {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioTimeoutHooks.class);
    private static final ThreadLocal<ScenarioWatchdog.Armed> ARMED = new ThreadLocal<>();

    @Before(order = 0)
    public void arm(Scenario scenario) {
        Duration timeout = AuttoSettings.get().properties().scenario().timeout();
        if (timeout.isZero()) {
            return;
        }
        Thread worker = Thread.currentThread();
        ARMED.set(ScenarioWatchdog.arm(timeout, () -> {
            LOG.error("Scenario '{}' exceeded autto.scenario.timeout ({}). Aborting it.", scenario.getName(),
                    timeout);
            DriverManager.abort(worker);
            worker.interrupt();
        }));
    }

    /** Runs last: after the evidence has been collected and the browser closed. */
    @After(order = 0)
    public void disarm() {
        ScenarioWatchdog.Armed armed = ARMED.get();
        if (armed == null) {
            return;
        }
        ARMED.remove();
        if (armed.disarm()) {
            Thread.interrupted(); // never leak the interrupt to the next scenario of this worker
            Report.fail("The scenario was aborted: it exceeded autto.scenario.timeout");
        }
    }
}
