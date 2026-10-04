package io.github.andercmd.autto.core.observability;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters of a test run. They are written as {@code metrics.json} and as a Prometheus text file
 * ({@code metrics.prom}, readable by the node_exporter textfile collector or any file-based scraper) next to the
 * report, and they feed the notification message.
 */
public final class RunMetrics {

    private static final RunMetrics GLOBAL = new RunMetrics();

    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger recovered = new AtomicInteger();
    private final AtomicInteger browserStartRetries = new AtomicInteger();
    private final AtomicLong scenarioMillis = new AtomicLong();
    private final AtomicLong slowestMillis = new AtomicLong();

    /** Process-wide metrics of the current run. */
    public static RunMetrics global() {
        return GLOBAL;
    }

    /** Records a finished scenario. {@code rerun} marks the second pass that repeats the failed scenarios. */
    public void scenarioFinished(Outcome outcome, Duration duration, boolean rerun) {
        switch (outcome) {
            case PASSED -> {
                passed.incrementAndGet();
                if (rerun) {
                    recovered.incrementAndGet();
                }
            }
            case FAILED -> failed.incrementAndGet();
            case SKIPPED -> skipped.incrementAndGet();
        }
        scenarioMillis.addAndGet(duration.toMillis());
        slowestMillis.accumulateAndGet(duration.toMillis(), Math::max);
    }

    public void browserStartRetried() {
        browserStartRetries.incrementAndGet();
    }

    public int passed() {
        return passed.get();
    }

    public int failed() {
        return failed.get();
    }

    public int skipped() {
        return skipped.get();
    }

    /** Scenarios that failed in the first pass and passed when they were run again (flaky). */
    public int recovered() {
        return recovered.get();
    }

    public int total() {
        return passed.get() + failed.get() + skipped.get();
    }

    public int browserStartRetries() {
        return browserStartRetries.get();
    }

    public Duration totalDuration() {
        return Duration.ofMillis(scenarioMillis.get());
    }

    public String toJson() {
        return """
                {
                  "scenarios": %d,
                  "passed": %d,
                  "failed": %d,
                  "skipped": %d,
                  "recovered_on_rerun": %d,
                  "browser_start_retries": %d,
                  "scenario_seconds_total": %s,
                  "scenario_seconds_max": %s
                }
                """.formatted(total(), passed(), failed(), skipped(), recovered(), browserStartRetries(),
                seconds(scenarioMillis.get()), seconds(slowestMillis.get()));
    }

    public String toPrometheus() {
        return """
                # HELP autto_scenarios_total Scenarios executed, by result.
                # TYPE autto_scenarios_total gauge
                autto_scenarios_total{result="passed"} %d
                autto_scenarios_total{result="failed"} %d
                autto_scenarios_total{result="skipped"} %d
                # HELP autto_scenarios_recovered_total Scenarios that failed first and passed on the rerun (flaky).
                # TYPE autto_scenarios_recovered_total gauge
                autto_scenarios_recovered_total %d
                # HELP autto_browser_start_retries_total Extra attempts needed to start a browser session.
                # TYPE autto_browser_start_retries_total gauge
                autto_browser_start_retries_total %d
                # HELP autto_scenario_seconds_total Summed duration of all scenarios.
                # TYPE autto_scenario_seconds_total gauge
                autto_scenario_seconds_total %s
                """.formatted(passed(), failed(), skipped(), recovered(), browserStartRetries(),
                seconds(scenarioMillis.get()));
    }

    private static String seconds(long millis) {
        return String.format(Locale.ROOT, "%.3f", millis / 1000.0);
    }

    /** Result of a scenario. */
    public enum Outcome {
        PASSED, FAILED, SKIPPED
    }
}
