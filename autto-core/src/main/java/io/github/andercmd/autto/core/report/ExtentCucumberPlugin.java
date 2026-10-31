package io.github.andercmd.autto.core.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.GherkinKeyword;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.gherkin.model.Feature;
import com.aventstack.extentreports.gherkin.model.Scenario;
import com.aventstack.extentreports.gherkin.model.ScenarioOutline;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.model.Test;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.DataTableArgument;
import io.cucumber.plugin.event.DocStringArgument;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.HookType;
import io.cucumber.plugin.event.Node;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.StepArgument;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.cucumber.plugin.event.TestSourceParsed;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.cucumber.plugin.event.WriteEvent;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.observability.Correlation;
import io.github.andercmd.autto.core.observability.RunSummaryPlugin;
import io.github.andercmd.autto.core.security.Secrets;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Cucumber plugin that turns the execution into a rich Extent Spark report.
 *
 * <p>Register it in {@code junit-platform.properties}:
 *
 * <pre>
 * cucumber.plugin=io.github.andercmd.autto.core.report.ExtentCucumberPlugin
 * </pre>
 *
 * <p>Features become top level tests, scenarios become child nodes and every Gherkin step becomes a grandchild node
 * with its own status, duration, data tables, doc strings, logs and attachments. Tags are mapped to categories,
 * {@code @author:name} tags to authors, and anything attached with {@code scenario.attach(...)} is rendered: images as
 * screenshots, videos as an embedded player, JSON/text as code blocks and any other file as a download link.
 *
 * <p>It is a {@link ConcurrentEventListener}, so it supports parallel execution.
 */
public final class ExtentCucumberPlugin implements ConcurrentEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ExtentCucumberPlugin.class);
    private static final String AUTHOR_TAG = "@author:";
    private static final String MDC_SCENARIO = "scenario";

    private final Map<URI, Node.Feature> featureNodes = new ConcurrentHashMap<>();
    private final Map<URI, ExtentTest> featureTests = new ConcurrentHashMap<>();
    private final Map<String, ExtentTest> outlineTests = new ConcurrentHashMap<>();
    private final Map<UUID, ScenarioReport> scenarios = new ConcurrentHashMap<>();
    private final Map<UUID, ExtentTest> steps = new ConcurrentHashMap<>();
    private ExtentReports extent;

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::onRunStarted);
        publisher.registerHandlerFor(TestSourceParsed.class, this::onSourceParsed);
        publisher.registerHandlerFor(TestCaseStarted.class, this::onCaseStarted);
        publisher.registerHandlerFor(TestStepStarted.class, this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
        publisher.registerHandlerFor(EmbedEvent.class, this::onEmbed);
        publisher.registerHandlerFor(WriteEvent.class, this::onWrite);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
    }

    // ------------------------------------------------------------------ run

    private void onRunStarted(TestRunStarted event) {
        extent = ExtentReportManager.get();
    }

    private void onRunFinished(TestRunFinished event) {
        ExtentReportManager.flush();
        LOG.info("Extent report: {}", ReportPaths.get().resolve("index.html").toUri());
    }

    private void onSourceParsed(TestSourceParsed event) {
        event.getNodes().stream()
                .filter(Node.Feature.class::isInstance)
                .map(Node.Feature.class::cast)
                .findFirst()
                .ifPresent(feature -> featureNodes.put(event.getUri(), feature));
    }

    // ------------------------------------------------------------------ scenarios

    private void onCaseStarted(TestCaseStarted event) {
        TestCase testCase = event.getTestCase();
        ExtentTest feature = featureTests.computeIfAbsent(testCase.getUri(), this::createFeature);

        int line = testCase.getLocation().getLine();
        String name = testCase.getName().isBlank() ? "Scenario at line " + line : testCase.getName();
        String description = "<span class='autto-location'>" + Html.escape(location(testCase)) + "</span>";
        Optional<List<Node>> path = findPath(testCase);
        Optional<Node> outline =
                path.flatMap(p -> p.stream().filter(Node.ScenarioOutline.class::isInstance).findFirst());

        ExtentTest scenario;
        ExtentTest parent = feature;
        if (outline.isPresent()) {
            String outlineKey = testCase.getUri() + ":" + outline.get().getLocation().getLine();
            ExtentTest outlineTest = outlineTests.computeIfAbsent(outlineKey,
                    key -> {
                        String outlineName = outline.get().getName().filter(n -> !n.isBlank()).orElse(name);
                        synchronized (feature) {
                            return feature.createNode(ScenarioOutline.class, Html.escape(outlineName), description);
                        }
                    });
            String example = path.map(p -> p.get(p.size() - 1)).flatMap(Node::getName).filter(n -> !n.isBlank())
                    .orElse("Example");
            parent = outlineTest;
            synchronized (outlineTest) {
                scenario = outlineTest.createNode(Scenario.class,
                        Html.escape(name) + " <span class='autto-example'>" + Html.escape(example) + " · line "
                                + line + "</span>",
                        description);
            }
        } else {
            synchronized (feature) {
                scenario = feature.createNode(Scenario.class, Html.escape(name), description);
            }
        }
        scenario.getModel().setStartTime(Date.from(event.getInstant()));

        List<String> categories = new ArrayList<>();
        List<String> authors = new ArrayList<>();
        for (String tag : testCase.getTags()) {
            if (tag.toLowerCase(Locale.ROOT).startsWith(AUTHOR_TAG)) {
                authors.add(tag.substring(AUTHOR_TAG.length()));
            } else {
                categories.add(tag);
            }
        }
        if (authors.isEmpty()) {
            Optional.ofNullable(AuttoSettings.get().properties().report().author()).ifPresent(authors::add);
        }
        if (!categories.isEmpty()) {
            scenario.assignCategory(categories.toArray(String[]::new));
        }
        if (!authors.isEmpty()) {
            scenario.assignAuthor(authors.toArray(String[]::new));
        }

        if (RunSummaryPlugin.isRerun()) {
            scenario.assignCategory("rerun");
        }

        ScenarioReport report = new ScenarioReport(scenario, parent);
        scenarios.put(testCase.getId(), report);
        ScenarioReport.bind(report);
        MDC.put(MDC_SCENARIO, testCase.getName());
        Correlation.begin();
    }

    private void onCaseFinished(TestCaseFinished event) {
        TestCase testCase = event.getTestCase();
        ScenarioReport report = scenarios.remove(testCase.getId());
        try {
            if (report == null) {
                return;
            }
            ExtentTest scenario = report.scenario();
            Result result = event.getResult();
            if (result.getStatus() == io.cucumber.plugin.event.Status.FAILED && scenario.getStatus() != Status.FAIL) {
                scenario.fail(result.getError() == null ? new AssertionError("Scenario failed") : result.getError());
            }
            scenario.getModel().setEndTime(Date.from(event.getInstant()));
            updateTimes(report.parent(), event.getInstant());
            Optional.ofNullable(featureTests.get(testCase.getUri()))
                    .filter(feature -> feature != report.parent())
                    .ifPresent(feature -> updateTimes(feature, event.getInstant()));
        } finally {
            testCase.getTestSteps().forEach(step -> steps.remove(step.getId()));
            ScenarioReport.unbind();
            MDC.remove(MDC_SCENARIO);
            Correlation.end();
        }
    }

    // ------------------------------------------------------------------ steps

    private void onStepStarted(TestStepStarted event) {
        ScenarioReport report = scenarios.get(event.getTestCase().getId());
        if (report == null) {
            return;
        }
        if (event.getTestStep() instanceof PickleStepTestStep step) {
            ExtentTest node = createStep(report.scenario(), step.getStep().getKeyword(), step.getStep().getText());
            node.getModel().setStartTime(Date.from(event.getInstant()));
            renderArgument(node, step.getStep().getArgument());
            steps.put(step.getId(), node);
            report.stepStarted(node);
        } else if (event.getTestStep() instanceof HookTestStep hook) {
            report.hookStarted(hook.getHookType() == HookType.AFTER_STEP || hook.getHookType() == HookType.BEFORE_STEP,
                    hook.getHookType() == HookType.BEFORE);
        }
    }

    private void onStepFinished(TestStepFinished event) {
        ScenarioReport report = scenarios.get(event.getTestCase().getId());
        if (report == null) {
            return;
        }
        Result result = event.getResult();
        if (event.getTestStep() instanceof PickleStepTestStep step) {
            ExtentTest node = steps.get(step.getId());
            if (node != null) {
                applyStepResult(node, result, step);
                node.getModel().setEndTime(Date.from(event.getInstant()));
            }
            report.stepFinished();
        } else if (event.getTestStep() instanceof HookTestStep hook) {
            if (result.getStatus() == io.cucumber.plugin.event.Status.FAILED) {
                ExtentTest target = report.target();
                target.fail(MarkupHelper.createLabel(hookLabel(hook), ExtentColor.RED));
                target.fail(result.getError());
            }
            report.hookFinished();
        }
    }

    private void applyStepResult(ExtentTest node, Result result, PickleStepTestStep step) {
        switch (result.getStatus()) {
            case PASSED -> node.getModel().setStatus(Status.PASS);
            case FAILED, AMBIGUOUS -> node.fail(result.getError() == null
                    ? new AssertionError(result.getStatus().name()) : result.getError());
            case SKIPPED -> node.skip("Step skipped");
            case PENDING -> node.warning(MarkupHelper.createLabel("Pending: " + message(result), ExtentColor.ORANGE));
            case UNDEFINED -> node.warning(MarkupHelper.createLabel(
                    "Undefined step. Implement a step definition matching: " + step.getStep().getText(),
                    ExtentColor.ORANGE));
            case UNUSED -> node.skip("Step not used");
        }
        Duration duration = result.getDuration();
        boolean passed = result.getStatus() == io.cucumber.plugin.event.Status.PASSED;
        if (duration != null && duration.toMillis() > 0 && !passed) {
            node.info("Duration: " + duration.toMillis() + " ms");
        }
    }

    // ------------------------------------------------------------------ attachments and logs

    private void onEmbed(EmbedEvent event) {
        ScenarioReport report = scenarios.get(event.getTestCase().getId());
        if (report == null) {
            return;
        }
        ExtentTest target = report.target();
        String mediaType = Optional.ofNullable(event.getMediaType()).orElse("application/octet-stream")
                .toLowerCase(Locale.ROOT);
        String name = Optional.ofNullable(event.getName()).filter(n -> !n.isBlank()).orElse("Attachment");
        byte[] data = event.getData();
        try {
            if (mediaType.startsWith("image/")) {
                target.log(Status.INFO, Html.escape(name), Report.media(name, data));
            } else if (mediaType.startsWith("video/")) {
                String extension = mediaType.substring("video/".length()).replaceAll("[^a-z0-9]", "");
                String relative = ReportPaths.get()
                        .store(ReportPaths.Kind.VIDEO, name, extension.isEmpty() ? "mp4" : extension, data);
                target.info(Html.video(relative, name));
            } else if (mediaType.equals("application/json")) {
                target.info("<b>" + Html.escape(name) + "</b>");
                target.info(MarkupHelper.createCodeBlock(Secrets.mask(new String(data, StandardCharsets.UTF_8)),
                        CodeLanguage.JSON));
            } else if (mediaType.equals("text/html")) {
                byte[] masked = Secrets.mask(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                String relative = ReportPaths.get().store(ReportPaths.Kind.ATTACHMENT, name, "html", masked);
                target.info(Html.link(relative, name + " (open)"));
            } else if (mediaType.startsWith("text/") || mediaType.endsWith("xml")) {
                target.info(Html.collapsible(name, new String(data, StandardCharsets.UTF_8), data.length < 4_000));
            } else {
                String relative =
                        ReportPaths.get().store(ReportPaths.Kind.ATTACHMENT, name, extension(mediaType), data);
                target.info(Html.link(relative, name));
            }
        } catch (RuntimeException e) {
            LOG.warn("Attachment '{}' could not be added to the report: {}", name, e.getMessage());
        }
    }

    private void onWrite(WriteEvent event) {
        ScenarioReport report = scenarios.get(event.getTestCase().getId());
        if (report != null) {
            report.target().info(Html.escape(event.getText()));
        }
    }

    // ------------------------------------------------------------------ helpers

    private ExtentTest createFeature(URI uri) {
        Node.Feature feature = featureNodes.get(uri);
        String name = Optional.ofNullable(feature).flatMap(Node::getName).filter(n -> !n.isBlank())
                .orElseGet(() -> fileName(uri));
        synchronized (extent) {
            return extent.createTest(Feature.class, Html.escape(name),
                    "<span class='autto-location'>" + Html.escape(relative(uri)) + "</span>");
        }
    }

    /** Parent nodes (features, outlines) span from their first child start to their last child end. */
    private static void updateTimes(ExtentTest node, Instant finished) {
        synchronized (node) {
            Test parent = node.getModel();
            Date end = Date.from(finished);
            parent.getChildren().stream()
                    .map(Test::getStartTime)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .ifPresent(parent::setStartTime);
            if (parent.getEndTime() == null || parent.getEndTime().before(end)) {
                parent.setEndTime(end);
            }
        }
    }

    private static ExtentTest createStep(ExtentTest scenario, String keyword, String text) {
        String trimmed = keyword.trim();
        String name = "<span class='autto-kw'>" + Html.escape(trimmed) + "</span> " + Html.escape(text);
        try {
            return scenario.createNode(new GherkinKeyword(trimmed), name);
        } catch (ClassNotFoundException | RuntimeException e) {
            // Keyword of a language not known by Extent: a plain node renders exactly the same.
            return scenario.createNode(name);
        }
    }

    private static void renderArgument(ExtentTest node, StepArgument argument) {
        if (argument instanceof DataTableArgument table) {
            String[][] cells = table.cells().stream()
                    .map(row -> row.stream().map(Html::escape).toArray(String[]::new))
                    .toArray(String[][]::new);
            node.info(MarkupHelper.createTable(cells, "table-sm autto-table"));
        } else if (argument instanceof DocStringArgument docString) {
            String type = Optional.ofNullable(docString.getMediaType()).orElse("");
            if (type.contains("json")) {
                node.info(MarkupHelper.createCodeBlock(Secrets.mask(docString.getContent()), CodeLanguage.JSON));
            } else if (type.contains("xml")) {
                node.info(MarkupHelper.createCodeBlock(Secrets.mask(docString.getContent()), CodeLanguage.XML));
            } else {
                node.info("<pre class='autto-pre'>" + Html.escape(docString.getContent()) + "</pre>");
            }
        }
    }

    /** Path from the feature down to the Gherkin node (scenario or example row) of a test case. */
    private Optional<List<Node>> findPath(TestCase testCase) {
        Node.Feature feature = featureNodes.get(testCase.getUri());
        if (feature == null) {
            return Optional.empty();
        }
        int line = testCase.getLocation().getLine();
        return feature.findPathTo(n -> n.getLocation().getLine() == line);
    }

    private static String hookLabel(HookTestStep hook) {
        return switch (hook.getHookType()) {
            case BEFORE -> "@Before hook failed";
            case AFTER -> "@After hook failed";
            case BEFORE_STEP -> "@BeforeStep hook failed";
            case AFTER_STEP -> "@AfterStep hook failed";
        } + " (" + hook.getCodeLocation() + ")";
    }

    private static String message(Result result) {
        return result.getError() == null ? "" : String.valueOf(result.getError().getMessage());
    }

    private static String location(TestCase testCase) {
        return relative(testCase.getUri()) + ":" + testCase.getLocation().getLine();
    }

    private static String relative(URI uri) {
        String value = uri.toString();
        int index = value.indexOf("features/");
        if (index >= 0) {
            return value.substring(index);
        }
        return value.replaceFirst("^classpath:", "");
    }

    private static String fileName(URI uri) {
        String path = uri.toString();
        return path.substring(path.lastIndexOf('/') + 1).replace(".feature", "");
    }

    private static String extension(String mediaType) {
        return switch (mediaType) {
            case "application/pdf" -> "pdf";
            case "application/zip" -> "zip";
            case "text/csv" -> "csv";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            default -> "bin";
        };
    }
}
