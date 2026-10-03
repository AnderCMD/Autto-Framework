package io.github.andercmd.autto.core.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.config.EvidenceMode;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.driver.DriverSession;
import io.github.andercmd.autto.core.media.Screenshots;
import io.github.andercmd.autto.core.report.Report;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Browser life-cycle and evidence collection for every UI scenario. Part of the framework glue: add
 * {@code io.github.andercmd.autto.core.cucumber} to {@code cucumber.glue}.
 *
 * <p>Scenarios tagged with {@code @nobrowser} (API, data or pure logic scenarios) are skipped.
 */
public class BrowserHooks {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserHooks.class);
    private static final String UI_SCENARIOS = "not @nobrowser";

    private final AuttoSettings settings;
    private final AuttoProperties.Evidence evidence;

    public BrowserHooks(AuttoSettings settings) {
        this.settings = settings;
        this.evidence = settings.properties().evidence();
    }

    @Before(value = UI_SCENARIOS, order = 10)
    public void startBrowser() {
        DriverSession session = DriverManager.start(settings);
        Report.device(session.details().label());
        if (evidence.video() != EvidenceMode.OFF) {
            session.startRecording(evidence.videoFps(), evidence.videoMaxDuration(), evidence.videoMaxWidth());
        }
    }

    @AfterStep(UI_SCENARIOS)
    public void stepScreenshot(Scenario scenario) {
        if (!DriverManager.isRunning() || !evidence.screenshot().shouldKeep(scenario.isFailed())) {
            return;
        }
        Screenshots.capture(DriverManager.driver()).ifPresent(png ->
                scenario.attach(png, "image/png", scenario.isFailed() ? "Screenshot at failure" : "Screenshot"));
    }

    @After(value = UI_SCENARIOS, order = 10)
    public void collectEvidenceAndQuit(Scenario scenario) {
        if (!DriverManager.isRunning()) {
            return;
        }
        try {
            DriverSession session = DriverManager.session();
            if (scenario.isFailed()) {
                attachFailureDetails(scenario, session);
            }
            attachVideo(scenario, session);
        } catch (RuntimeException e) {
            LOG.warn("Evidence could not be collected for '{}': {}", scenario.getName(), e.toString());
        } finally {
            DriverManager.quit();
        }
    }

    private void attachFailureDetails(Scenario scenario, DriverSession session) {
        try {
            scenario.log("URL at failure: " + session.driver().getCurrentUrl());
            if (evidence.pageSource()) {
                String source = session.driver().getPageSource();
                if (source != null) {
                    scenario.attach(source.getBytes(StandardCharsets.UTF_8), "text/html", "Page source");
                }
            }
        } catch (RuntimeException e) {
            LOG.debug("Page details unavailable: {}", e.getMessage());
        }
        List<String> console = session.consoleEntries();
        if (!console.isEmpty()) {
            scenario.attach(String.join("\n", console).getBytes(StandardCharsets.UTF_8), "text/plain",
                    "Browser console (" + console.size() + " entries)");
        }
    }

    private void attachVideo(Scenario scenario, DriverSession session) {
        if (!session.isRecording()) {
            return;
        }
        boolean keep = evidence.video().shouldKeep(scenario.isFailed());
        Path temp = null;
        try {
            temp = Files.createTempFile("autto-video-", ".mp4");
            Path video = session.stopRecording(keep, temp).orElse(null);
            if (video != null && Files.size(video) > 0) {
                scenario.attach(Files.readAllBytes(video), "video/mp4", "Scenario recording");
            }
        } catch (IOException e) {
            LOG.warn("Video could not be attached: {}", e.getMessage());
        } finally {
            deleteQuietly(temp);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.debug("Temporary file {} not deleted: {}", file, e.getMessage());
        }
    }
}
