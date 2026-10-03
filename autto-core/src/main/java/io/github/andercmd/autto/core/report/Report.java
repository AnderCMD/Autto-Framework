package io.github.andercmd.autto.core.report;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.model.Media;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.media.Screenshots;
import io.github.andercmd.autto.core.security.Secrets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public, static API to enrich the Extent report from step definitions, page objects or hooks.
 *
 * <pre>{@code
 * Report.info("Searching product " + name);
 * Report.screenshot("Search results");
 * Report.json("Response", body);
 * Report.table("Order", Map.of("id", "42", "total", "$10"));
 * }</pre>
 *
 * <p>Every call is also written to the execution log, and all methods are no-ops when no scenario is running, so
 * they are always safe to call. Registered {@link Secrets} are masked.
 */
public final class Report {

    private static final Logger LOG = LoggerFactory.getLogger(Report.class);

    private Report() {
    }

    public static void info(String message) {
        LOG.info(Secrets.mask(message));
        target().ifPresent(t -> t.info(Html.escape(message)));
    }

    public static void pass(String message) {
        LOG.info("PASS: {}", Secrets.mask(message));
        target().ifPresent(t -> t.pass(MarkupHelper.createLabel(Html.escape(message), ExtentColor.GREEN)));
    }

    public static void warning(String message) {
        LOG.warn(Secrets.mask(message));
        target().ifPresent(t -> t.warning(MarkupHelper.createLabel(Html.escape(message), ExtentColor.ORANGE)));
    }

    /** Marks the current step as failed without throwing (soft failure). */
    public static void fail(String message) {
        LOG.error(Secrets.mask(message));
        target().ifPresent(t -> t.fail(MarkupHelper.createLabel(Html.escape(message), ExtentColor.RED)));
    }

    /** Adds raw HTML (trusted content only). */
    public static void html(String html) {
        target().ifPresent(t -> t.info(Secrets.mask(html)));
    }

    public static void code(String title, String content) {
        LOG.debug("{}:\n{}", title, Secrets.mask(content));
        target().ifPresent(t -> t.info(Html.collapsible(title, content, true)));
    }

    public static void json(String title, String json) {
        String masked = Secrets.mask(json);
        LOG.debug("{}: {}", title, masked);
        target().ifPresent(t -> {
            t.info("<b>" + Html.escape(title) + "</b>");
            t.info(MarkupHelper.createCodeBlock(masked, CodeLanguage.JSON));
        });
    }

    public static void table(String title, Map<String, ?> rows) {
        String[][] data = rows.entrySet().stream()
                .map(e -> new String[] {Html.escape(e.getKey()), Html.escape(String.valueOf(e.getValue()))})
                .toArray(String[][]::new);
        table(title, data);
    }

    public static void table(String title, List<List<String>> rows) {
        String[][] data = rows.stream()
                .map(row -> row.stream().map(Html::escape).toArray(String[]::new))
                .toArray(String[][]::new);
        table(title, data);
    }

    private static void table(String title, String[][] data) {
        target().ifPresent(t -> {
            t.info("<b>" + Html.escape(title) + "</b>");
            t.info(MarkupHelper.createTable(data, "table-sm autto-table"));
        });
    }

    /** Takes a screenshot of the current browser (if any) and adds it to the current step. */
    public static void screenshot(String title) {
        if (!DriverManager.isRunning()) {
            return;
        }
        Screenshots.capture(DriverManager.driver()).ifPresent(png -> image(title, png, Status.INFO));
    }

    /** Adds a PNG image to the current step. */
    public static void image(String title, byte[] png, Status status) {
        target().ifPresent(t -> t.log(status, Html.escape(title), media(title, png)));
    }

    /** Stores a video (MP4 bytes) next to the report and embeds a player in the current node. */
    public static void video(String title, byte[] mp4) {
        target().ifPresent(t -> {
            String relative = ReportPaths.get().store(ReportPaths.Kind.VIDEO, title, "mp4", mp4);
            t.info(Html.video(relative, title));
        });
    }

    /** Stores any file next to the report and adds a link to it. */
    public static void file(String title, byte[] content, String extension) {
        target().ifPresent(t -> {
            String relative = ReportPaths.get().store(ReportPaths.Kind.ATTACHMENT, title, extension, content);
            t.info(Html.link(relative, title));
        });
    }

    /** Adds the author(s) to the current scenario (shown in the "Author" view). */
    public static void author(String... authors) {
        ScenarioReport.current().ifPresent(r -> r.scenario().assignAuthor(authors));
    }

    /** Adds the device(s) to the current scenario (shown in the "Device" view). */
    public static void device(String... devices) {
        ScenarioReport.current().ifPresent(r -> r.scenario().assignDevice(devices));
    }

    /** Whether a scenario is being reported on the current thread. */
    public static boolean isActive() {
        return ScenarioReport.current().isPresent();
    }

    static Media media(String title, byte[] png) {
        if (AuttoSettings.get().properties().report().screenshotsBase64()) {
            String base64 = Base64.getEncoder().encodeToString(png);
            return MediaEntityBuilder.createScreenCaptureFromBase64String(base64, title).build();
        }
        String relative = ReportPaths.get().store(ReportPaths.Kind.SCREENSHOT, title, "png", png);
        return MediaEntityBuilder.createScreenCaptureFromPath(relative, title).build();
    }

    private static Optional<ExtentTest> target() {
        return ScenarioReport.current().map(ScenarioReport::target);
    }
}
