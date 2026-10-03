package io.github.andercmd.autto.core.media;

import java.util.Optional;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Safe screenshot capture: evidence collection must never be the reason a test fails. */
public final class Screenshots {

    private static final Logger LOG = LoggerFactory.getLogger(Screenshots.class);

    private Screenshots() {
    }

    /** Takes a PNG screenshot of the current viewport, or returns empty when the driver cannot provide one. */
    public static Optional<byte[]> capture(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot camera)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(camera.getScreenshotAs(OutputType.BYTES));
        } catch (WebDriverException | IllegalStateException e) {
            LOG.debug("Screenshot could not be taken: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
