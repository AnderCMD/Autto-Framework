package io.github.andercmd.autto.core.report;

import com.aventstack.extentreports.ExtentTest;
import java.util.Optional;

/**
 * Report nodes of the scenario that is currently running on this thread.
 *
 * <p>The {@link ExtentCucumberPlugin} receives Cucumber events on the thread executing the scenario, so a
 * {@link ThreadLocal} is enough to let page objects, step definitions and hooks log into the right node, even when
 * scenarios run in parallel.
 *
 * <p>The Spark BDD view only renders the logs of step nodes, so anything logged by a {@code @Before} or
 * {@code @After} hook goes to a dedicated node ("Setup" / "Scenario evidence") created on demand.
 */
final class ScenarioReport {

    private static final ThreadLocal<ScenarioReport> CURRENT = new ThreadLocal<>();

    enum Phase {
        BEFORE,
        STEPS,
        STEP_HOOK,
        AFTER
    }

    private final ExtentTest scenario;
    private final ExtentTest parent;
    private volatile ExtentTest currentStep;
    private volatile ExtentTest lastStep;
    private volatile ExtentTest setupNode;
    private volatile ExtentTest evidenceNode;
    private volatile Phase phase = Phase.BEFORE;

    ScenarioReport(ExtentTest scenario, ExtentTest parent) {
        this.scenario = scenario;
        this.parent = parent;
    }

    static Optional<ScenarioReport> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    static void bind(ScenarioReport report) {
        CURRENT.set(report);
    }

    static void unbind() {
        CURRENT.remove();
    }

    ExtentTest scenario() {
        return scenario;
    }

    /** Feature or Scenario Outline node that contains the scenario. */
    ExtentTest parent() {
        return parent;
    }

    void stepStarted(ExtentTest step) {
        currentStep = step;
        lastStep = step;
        phase = Phase.STEPS;
    }

    void stepFinished() {
        currentStep = null;
    }

    void hookStarted(boolean stepHook, boolean beforeHook) {
        if (stepHook) {
            phase = Phase.STEP_HOOK;
        } else {
            phase = beforeHook ? Phase.BEFORE : Phase.AFTER;
        }
    }

    void hookFinished() {
        if (phase == Phase.STEP_HOOK) {
            phase = Phase.STEPS;
        }
    }

    /**
     * Where a log or attachment should go: the running step; the step just finished when we are inside an
     * {@code @AfterStep} hook (so failure screenshots appear right under the failing step); or the setup / evidence
     * node for {@code @Before} / {@code @After} hooks.
     */
    synchronized ExtentTest target() {
        ExtentTest step = currentStep;
        if (step != null) {
            return step;
        }
        return switch (phase) {
            case STEP_HOOK, STEPS -> lastStep != null ? lastStep : setup();
            case BEFORE -> setup();
            case AFTER -> evidence();
        };
    }

    private ExtentTest setup() {
        if (setupNode == null) {
            setupNode = scenario.createNode("<span class='autto-kw autto-kw-hook'>Setup</span> hooks");
        }
        return setupNode;
    }

    private ExtentTest evidence() {
        if (evidenceNode == null) {
            evidenceNode = scenario.createNode("<span class='autto-kw autto-kw-hook'>Evidence</span> scenario evidence");
        }
        return evidenceNode;
    }
}
