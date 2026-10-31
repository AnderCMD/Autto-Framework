package io.github.andercmd.autto.core.visual;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.report.Report;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import com.aventstack.extentreports.Status;

/**
 * Pixel comparison of screenshots against approved baselines.
 *
 * <pre>{@code
 * VisualRegression.assertMatches("login-page");                                  // whole viewport
 * VisualRegression.assertMatches("inventory", new Rectangle(0, 0, 1920, 120));  // ignore the header area
 * }</pre>
 *
 * <p>The first run has no baseline: run with {@code -Dautto.visual.update=true} to create it, review the image and
 * commit it. Failed comparisons write the actual and a highlighted diff image next to the report.
 * Differences below {@code autto.visual.pixel-threshold} (per channel) and up to {@code autto.visual.tolerance} of
 * different pixels are accepted, which absorbs anti-aliasing noise. Use the same browser, size and OS for baselines
 * and runs (the Docker target is the most reproducible).
 */
public final class VisualRegression {

    private static final Duration STABILITY_TIMEOUT = Duration.ofSeconds(5);

    private VisualRegression() {
    }

    /** Compares the current viewport against the baseline {@code name}. */
    public static void assertMatches(String name, Rectangle... ignoredAreas) {
        assertMatches(name, stableScreenshot(), AuttoSettings.get().properties().visual(), ignoredAreas);
    }

    /**
     * Screenshot of a settled page: waits for web fonts and then for two consecutive screenshots to be identical,
     * so animations, late layout shifts and font swaps do not produce false differences.
     */
    static byte[] stableScreenshot() {
        WebDriver driver = DriverManager.driver();
        ((JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[arguments.length - 1];"
                        + "(document.fonts ? document.fonts.ready : Promise.resolve()).then(() => done(true));");
        TakesScreenshot camera = (TakesScreenshot) driver;
        byte[] previous = camera.getScreenshotAs(OutputType.BYTES);
        Instant deadline = Instant.now().plus(STABILITY_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            pause();
            byte[] current = camera.getScreenshotAs(OutputType.BYTES);
            if (Arrays.equals(previous, current)) {
                return current;
            }
            previous = current;
        }
        return previous;
    }

    private static void pause() {
        try {
            TimeUnit.MILLISECONDS.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the page to settle", e);
        }
    }

    static void assertMatches(String name, byte[] actualPng, AuttoProperties.Visual config, Rectangle... ignored) {
        Path baseline = Path.of(config.baselineDir()).resolve(name + ".png");
        try {
            if (config.update() || !Files.exists(baseline)) {
                if (!config.update()) {
                    throw new AssertionError("No visual baseline '" + baseline + "'. Run once with "
                            + "-Dautto.visual.update=true, review the image and commit it.");
                }
                Files.createDirectories(baseline.getParent());
                Files.write(baseline, actualPng);
                Report.info("Visual baseline written: " + baseline);
                return;
            }
            Result result = compare(ImageIO.read(baseline.toFile()), read(actualPng), config.pixelThreshold(),
                    List.of(ignored));
            if (result.ratio() > config.tolerance()) {
                Path dir = Path.of(config.diffDir());
                Files.createDirectories(dir);
                Files.write(dir.resolve(name + "-actual.png"), actualPng);
                Files.write(dir.resolve(name + "-diff.png"), toPng(result.diff()));
                Report.image("Visual diff: " + name, toPng(result.diff()), Status.FAIL);
                throw new AssertionError("Visual regression in '%s': %.3f%% of the pixels differ (tolerance %.3f%%). "
                        .formatted(name, result.ratio() * 100, config.tolerance() * 100)
                        + "Diff written to " + dir.resolve(name + "-diff.png"));
            }
            Report.info("Visual check '%s' passed (%.3f%% different)".formatted(name, result.ratio() * 100));
        } catch (IOException e) {
            throw new UncheckedIOException("Visual comparison '" + name + "' failed", e);
        }
    }

    /** Compares two images; differing sizes are a full mismatch. */
    static Result compare(BufferedImage expected, BufferedImage actual, int threshold, List<Rectangle> ignored) {
        if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) {
            BufferedImage diff = new BufferedImage(actual.getWidth(), actual.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = diff.createGraphics();
            g.setColor(Color.RED);
            g.fillRect(0, 0, diff.getWidth(), diff.getHeight());
            g.dispose();
            return new Result(1.0, diff);
        }
        int width = actual.getWidth();
        int height = actual.getHeight();
        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        long different = 0;
        long compared = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isIgnored(ignored, x, y)) {
                    diff.setRGB(x, y, 0x40_80_80_80);
                    continue;
                }
                compared++;
                if (differs(expected.getRGB(x, y), actual.getRGB(x, y), threshold)) {
                    different++;
                    diff.setRGB(x, y, 0xFF_FF_00_00);
                } else {
                    diff.setRGB(x, y, (actual.getRGB(x, y) & 0x00_FF_FF_FF) | 0x30_00_00_00);
                }
            }
        }
        return new Result(compared == 0 ? 0 : (double) different / compared, diff);
    }

    private static boolean differs(int a, int b, int threshold) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) > threshold
                || Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) > threshold
                || Math.abs((a & 0xFF) - (b & 0xFF)) > threshold;
    }

    private static boolean isIgnored(List<Rectangle> ignored, int x, int y) {
        for (Rectangle rectangle : ignored) {
            if (rectangle.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static BufferedImage read(byte[] png) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null) {
            throw new IOException("Not a valid image");
        }
        return image;
    }

    private static byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** Share of different pixels (0-1) and the highlighted diff image. */
    record Result(double ratio, BufferedImage diff) {

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%.4f", ratio);
        }
    }
}
