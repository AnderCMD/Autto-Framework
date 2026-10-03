package io.github.andercmd.autto.core.report;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestSourceParsed;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.security.Secrets;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes the run in the Allure results format so it can be viewed with {@code allure serve target/allure-results}
 * (or any Allure server). It is Autto's own writer: the official {@code allure-cucumber7-jvm} plugin does not work
 * with the Cucumber 8 message API.
 *
 * <pre>
 * cucumber.plugin=...,io.github.andercmd.autto.core.report.AllureResultsPlugin
 * </pre>
 *
 * <p>Steps, tags (as {@code tag} labels), feature names, failure messages with stack traces and every attachment
 * (screenshots, video, page source) are included; secrets are masked. The folder is
 * {@code target/allure-results} or {@code -Dautto.allure.results=<dir>}.
 */
public final class AllureResultsPlugin implements ConcurrentEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(AllureResultsPlugin.class);

    private final Map<java.net.URI, String> features = new ConcurrentHashMap<>();
    private final Map<UUID, AllureResult> results = new ConcurrentHashMap<>();
    private final Map<UUID, AllureResult.Step> steps = new ConcurrentHashMap<>();
    private final Map<UUID, AllureResult.Step> openSteps = new ConcurrentHashMap<>();
    private volatile Path dir;

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestSourceParsed.class, event -> event.getNodes().stream()
                .filter(io.cucumber.plugin.event.Node.Feature.class::isInstance)
                .findFirst()
                .flatMap(io.cucumber.plugin.event.Node::getName)
                .ifPresent(name -> features.put(event.getUri(), name)));
        publisher.registerHandlerFor(TestCaseStarted.class, this::onCaseStarted);
        publisher.registerHandlerFor(TestStepStarted.class, this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
        publisher.registerHandlerFor(EmbedEvent.class, this::onEmbed);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
    }

    private Path dir() {
        Path local = dir;
        if (local == null) {
            local = Path.of(AuttoSettings.get().find("autto.allure.results").orElse("target/allure-results"));
            try {
                Files.createDirectories(local);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            dir = local;
        }
        return local;
    }

    private void onCaseStarted(TestCaseStarted event) {
        TestCase testCase = event.getTestCase();
        String feature = features.getOrDefault(testCase.getUri(), "Feature");
        String name = testCase.getName().isBlank() ? "Scenario at line " + testCase.getLocation().getLine()
                : testCase.getName();
        AllureResult result = new AllureResult(UUID.randomUUID().toString(), name,
                feature + ": " + name + " (" + testCase.getUri() + ":" + testCase.getLocation().getLine() + ")",
                event.getInstant().toEpochMilli());
        result.label("framework", "autto");
        result.label("language", "gherkin");
        result.label("feature", feature);
        result.label("suite", feature);
        result.label("thread", Thread.currentThread().getName());
        testCase.getTags().forEach(tag -> result.label("tag", tag.replaceFirst("^@", "")));
        results.put(testCase.getId(), result);
    }

    private void onStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep step
                && results.containsKey(event.getTestCase().getId())) {
            AllureResult.Step allureStep = new AllureResult.Step(
                    (step.getStep().getKeyword() + step.getStep().getText()).trim(), event.getInstant().toEpochMilli());
            steps.put(step.getId(), allureStep);
            openSteps.put(event.getTestCase().getId(), allureStep);
        }
    }

    private void onStepFinished(TestStepFinished event) {
        AllureResult result = results.get(event.getTestCase().getId());
        if (result == null) {
            return;
        }
        Result outcome = event.getResult();
        if (event.getTestStep() instanceof PickleStepTestStep step) {
            AllureResult.Step allureStep = steps.remove(step.getId());
            openSteps.remove(event.getTestCase().getId());
            if (allureStep != null) {
                allureStep.stop = event.getInstant().toEpochMilli();
                allureStep.status = AllureResult.status(outcome.getStatus().name(), outcome.getError());
                if (outcome.getError() != null) {
                    allureStep.message = Secrets.mask(String.valueOf(outcome.getError().getMessage()));
                }
                result.steps.add(allureStep);
            }
        }
        if (outcome.getError() != null && !"passed".equals(AllureResult.status(outcome.getStatus().name(),
                outcome.getError())) && result.message == null) {
            result.status = AllureResult.status(outcome.getStatus().name(), outcome.getError());
            result.message = Secrets.mask(String.valueOf(outcome.getError().getMessage()));
            result.trace = Secrets.mask(stackTrace(outcome.getError()));
        } else if (outcome.getError() != null && result.message == null) {
            result.message = Secrets.mask(String.valueOf(outcome.getError().getMessage()));
        }
    }

    private void onEmbed(EmbedEvent event) {
        AllureResult result = results.get(event.getTestCase().getId());
        if (result == null) {
            return;
        }
        String mediaType = event.getMediaType() == null ? "application/octet-stream" : event.getMediaType();
        String source = UUID.randomUUID() + "-attachment." + extension(mediaType);
        byte[] data = event.getData();
        if (mediaType.startsWith("text/") || mediaType.contains("json") || mediaType.contains("xml")) {
            data = Secrets.mask(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        }
        try {
            Files.write(dir().resolve(source), data);
        } catch (IOException e) {
            LOG.warn("Allure attachment could not be written: {}", e.getMessage());
            return;
        }
        String name = event.getName() == null || event.getName().isBlank() ? "Attachment" : event.getName();
        Map<String, String> attachment = Map.of("name", name, "source", source, "type", mediaType);
        AllureResult.Step open = openSteps.get(event.getTestCase().getId());
        (open != null ? open.attachments : result.attachments).add(attachment);
    }

    private void onCaseFinished(TestCaseFinished event) {
        AllureResult result = results.remove(event.getTestCase().getId());
        if (result == null) {
            return;
        }
        Result outcome = event.getResult();
        result.stop = event.getInstant().toEpochMilli();
        if (result.message == null || !"passed".equals(AllureResult.status(outcome.getStatus().name(),
                outcome.getError()))) {
            result.status = AllureResult.status(outcome.getStatus().name(), outcome.getError());
        }
        if (result.steps.stream().anyMatch(s -> "skipped".equals(s.status)) && "passed".equals(result.status)) {
            result.status = "skipped";
        }
        try {
            Files.writeString(dir().resolve(result.uuid + "-result.json"), result.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Allure result could not be written: {}", e.getMessage());
        }
    }

    private static String stackTrace(Throwable error) {
        StringWriter out = new StringWriter();
        error.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    private static String extension(String mediaType) {
        return switch (mediaType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "video/mp4" -> "mp4";
            case "text/html" -> "html";
            case "text/plain" -> "txt";
            case "application/json" -> "json";
            default -> "bin";
        };
    }
}
