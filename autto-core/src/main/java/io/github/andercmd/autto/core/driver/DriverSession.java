package io.github.andercmd.autto.core.driver;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.media.Screenshots;
import io.github.andercmd.autto.core.media.VideoRecorder;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.LogInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A running browser/device plus everything attached to it: video recorder and console log listener. */
public final class DriverSession {

    private static final Logger LOG = LoggerFactory.getLogger(DriverSession.class);

    private final WebDriver driver;
    private final Runnable closer;
    private final String description;
    private final SessionDetails details;
    private final List<String> consoleEntries = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean closed = new AtomicBoolean();
    private VideoRecorder recorder;
    private LogInspector logInspector;

    DriverSession(DriverHandle handle, AuttoProperties props) {
        this.driver = handle.driver();
        this.closer = handle.closer();
        this.description = handle.description();
        this.details = SessionDetails.of(driver);
        if (props.browser().consoleLogs()) {
            listenToConsole();
        }
    }

    /** How the session was started ("local chrome", "docker firefox", ...). */
    public String description() {
        return description;
    }

    public WebDriver driver() {
        return driver;
    }

    public SessionDetails details() {
        return details;
    }

    /** Starts recording a video of the session. */
    public void startRecording(int fps, Duration maxDuration, int maxWidth) {
        if (recorder == null) {
            recorder = new VideoRecorder(() -> Screenshots.capture(driver), fps, maxDuration, maxWidth);
            recorder.start();
        }
    }

    /** Stops the recording and writes it to {@code target} when {@code keep} is true. */
    public Optional<Path> stopRecording(boolean keep, Path target) {
        if (recorder == null) {
            return Optional.empty();
        }
        VideoRecorder current = recorder;
        recorder = null;
        if (!keep) {
            current.discard();
            return Optional.empty();
        }
        current.stop();
        return current.save(target);
    }

    public boolean isRecording() {
        return recorder != null;
    }

    /** Console messages and JavaScript errors collected through WebDriver BiDi (if enabled). */
    public List<String> consoleEntries() {
        synchronized (consoleEntries) {
            return List.copyOf(consoleEntries);
        }
    }

    void quit() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (recorder != null) {
            recorder.discard();
            recorder = null;
        }
        if (logInspector != null) {
            try {
                logInspector.close();
            } catch (RuntimeException e) {
                LOG.debug("Log inspector could not be closed: {}", e.getMessage());
            }
        }
        closer.run();
    }

    private void listenToConsole() {
        if (!(driver instanceof HasBiDi)) {
            LOG.warn("browser.console.logs is enabled but this driver does not support WebDriver BiDi");
            return;
        }
        try {
            logInspector = new LogInspector(driver);
            logInspector.onConsoleEntry(entry -> consoleEntries.add(
                    format(entry.getTimestamp(), String.valueOf(entry.getLevel()), entry.getText())));
            logInspector.onJavaScriptException(entry -> consoleEntries.add(
                    format(entry.getTimestamp(), "JS-EXCEPTION", entry.getText())));
        } catch (RuntimeException e) {
            LOG.warn("Unable to listen to browser console logs: {}", e.getMessage());
        }
    }

    private static String format(long epochMillis, String level, String text) {
        return Instant.ofEpochMilli(epochMillis) + " [" + level + "] " + text;
    }
}
