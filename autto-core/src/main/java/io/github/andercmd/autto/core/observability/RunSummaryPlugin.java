package io.github.andercmd.autto.core.observability;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.report.ReportPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber plugin that, when the run ends, writes {@code metrics.json} and {@code metrics.prom} next to the report
 * and sends the summary to the webhook configured in {@code autto.notifications.*}.
 *
 * <pre>
 * cucumber.plugin=io.github.andercmd.autto.core.observability.RunSummaryPlugin
 * </pre>
 *
 * <p>The second pass that repeats failed scenarios ({@code -Dautto.rerun=true}) marks the scenarios it recovers as
 * flaky.
 */
public final class RunSummaryPlugin implements ConcurrentEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(RunSummaryPlugin.class);

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, event -> onRunFinished());
    }

    private void onCaseFinished(TestCaseFinished event) {
        Status status = event.getResult().getStatus();
        RunMetrics.Outcome outcome = switch (status) {
            case PASSED -> RunMetrics.Outcome.PASSED;
            case SKIPPED, PENDING, UNUSED -> RunMetrics.Outcome.SKIPPED;
            default -> RunMetrics.Outcome.FAILED;
        };
        RunMetrics.global().scenarioFinished(outcome, event.getResult().getDuration(), isRerun());
    }

    private void onRunFinished() {
        RunMetrics metrics = RunMetrics.global();
        write("metrics.json", metrics.toJson());
        write("metrics.prom", metrics.toPrometheus());
        Notifier.send(AuttoSettings.get().properties().notifications(), metrics);
    }

    /** Whether this is the second pass that repeats the failed scenarios of the first one. */
    public static boolean isRerun() {
        return Boolean.parseBoolean(AuttoSettings.get().find("autto.rerun").orElse("false"));
    }

    private static void write(String name, String content) {
        try {
            Path file = ReportPaths.get().resolve(name);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            LOG.warn("{} could not be written: {}", name, e.getMessage());
        }
    }
}
