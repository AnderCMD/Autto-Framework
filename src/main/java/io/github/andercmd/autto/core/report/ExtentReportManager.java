package io.github.andercmd.autto.core.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.JsonFormatter;
import com.aventstack.extentreports.reporter.configuration.ExtentSparkReporterConfig;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;
import io.github.andercmd.autto.core.config.AuttoConfig;
import io.github.andercmd.autto.core.config.ConfigKeys;
import io.github.andercmd.autto.core.driver.DriverFactory;
import io.github.andercmd.autto.core.driver.ExecutionTarget;
import io.github.andercmd.autto.core.media.EvidenceMode;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns the single {@link ExtentReports} instance of the JVM and its reporters. */
public final class ExtentReportManager {

    private static final Logger LOG = LoggerFactory.getLogger(ExtentReportManager.class);
    private static final ViewName[] VIEW_ORDER = {
        ViewName.DASHBOARD, ViewName.TEST, ViewName.CATEGORY, ViewName.DEVICE, ViewName.AUTHOR, ViewName.EXCEPTION,
        ViewName.LOG
    };

    private static ExtentReports extent;

    private ExtentReportManager() {
    }

    public static synchronized ExtentReports get() {
        if (extent == null) {
            extent = create(AuttoConfig.get(), ReportPaths.get());
        }
        return extent;
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }

    /** Adds (or replaces) a row of the "Environment" table of the dashboard. */
    public static void systemInfo(String key, String value) {
        if (value != null && !value.isBlank()) {
            get().setSystemInfo(key, value);
        }
    }

    private static ExtentReports create(AuttoConfig config, ReportPaths paths) {
        ExtentReports reports = new ExtentReports();
        reports.setReportUsesManualConfiguration(true);

        ExtentSparkReporter full = spark(config, paths, "index.html", config.get(ConfigKeys.REPORT_NAME, "Execution report"));
        ExtentSparkReporter failed = spark(config, paths, "failed.html", "Failures only");
        failed.filter().statusFilter().as(new Status[] {Status.FAIL, Status.WARNING}).apply();
        JsonFormatter json = new JsonFormatter(paths.resolve("extent.json").toFile());
        reports.attachReporter(full, failed, json);

        environment(config).forEach(reports::setSystemInfo);
        LOG.info("Extent report will be written to {}", paths.resolve("index.html").toUri());
        return reports;
    }

    private static ExtentSparkReporter spark(AuttoConfig config, ReportPaths paths, String file, String reportName) {
        ExtentSparkReporter spark = new ExtentSparkReporter(paths.resolve(file).toFile());
        spark.viewConfigurer().viewOrder().as(VIEW_ORDER).apply();
        ExtentSparkReporterConfig conf = spark.config();
        conf.setTheme(config.get(ConfigKeys.REPORT_THEME, "dark").equalsIgnoreCase("dark") ? Theme.DARK : Theme.STANDARD);
        conf.setDocumentTitle(config.get(ConfigKeys.REPORT_TITLE, "Autto · Test Automation Report"));
        conf.setReportName(reportName);
        conf.setEncoding(StandardCharsets.UTF_8.name());
        conf.setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        conf.setTimelineEnabled(config.getBoolean(ConfigKeys.REPORT_TIMELINE, true));
        conf.enableOfflineMode(config.getBoolean(ConfigKeys.REPORT_OFFLINE, true));
        conf.thumbnailForBase64(true);
        resource("autto/report/autto.css").ifPresent(conf::setCss);
        resource("autto/report/autto.js").ifPresent(conf::setJs);
        return spark;
    }

    static Map<String, String> environment(AuttoConfig config) {
        Map<String, String> info = new LinkedHashMap<>();
        ExecutionTarget target = config.getEnum(ConfigKeys.EXECUTION_TARGET, ExecutionTarget.class, ExecutionTarget.LOCAL);
        info.put("Environment", config.get(ConfigKeys.ENV, "qa").toUpperCase(Locale.ROOT));
        config.find(ConfigKeys.BASE_URL).ifPresent(url -> info.put("Base URL", url));
        info.put("Execution target", target.name().toLowerCase(Locale.ROOT));
        if (target == ExecutionTarget.APPIUM) {
            info.put("Mobile platform", config.get(ConfigKeys.APPIUM_PLATFORM, "android"));
            info.put("Appium server", DriverFactory.redact(config.get(ConfigKeys.APPIUM_URL, "http://127.0.0.1:4723")));
        } else {
            info.put("Browser", config.get(ConfigKeys.BROWSER, "chrome")
                    + config.find(ConfigKeys.BROWSER_VERSION).map(v -> " " + v).orElse(""));
            info.put("Headless", String.valueOf(config.getBoolean(ConfigKeys.BROWSER_HEADLESS, false)));
            config.find(ConfigKeys.BROWSER_MOBILE_EMULATION).ifPresent(d -> info.put("Mobile emulation", d));
        }
        if (target == ExecutionTarget.REMOTE) {
            info.put("Remote URL", DriverFactory.redact(config.get(ConfigKeys.REMOTE_URL, "http://localhost:4444")));
        }
        config.find(ConfigKeys.PLATFORM_NAME).ifPresent(p -> info.put("Requested platform", p));
        info.put("Screenshots", config.getEnum(ConfigKeys.SCREENSHOT_MODE, EvidenceMode.class, EvidenceMode.ON_FAILURE)
                .name().toLowerCase(Locale.ROOT));
        info.put("Videos", config.getEnum(ConfigKeys.VIDEO_MODE, EvidenceMode.class, EvidenceMode.ON_FAILURE)
                .name().toLowerCase(Locale.ROOT));
        config.find("cucumber.filter.tags").ifPresent(tags -> info.put("Tags", tags));
        info.put("Parallel", config.get("cucumber.execution.parallel.enabled", "false"));
        info.put("Operating system", System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");
        info.put("Java", System.getProperty("java.version") + " · " + System.getProperty("java.vendor"));
        version(org.openqa.selenium.WebDriver.class).ifPresent(v -> info.put("Selenium", v));
        version(io.cucumber.plugin.Plugin.class).ifPresent(v -> info.put("Cucumber", v));
        if (config.getBoolean(ConfigKeys.REPORT_SHOW_HOST, true)) {
            info.put("User", System.getProperty("user.name"));
            hostName().ifPresent(h -> info.put("Host", h));
        }
        ci().ifPresent(ci -> info.put("CI", ci));
        config.withPrefix(ConfigKeys.REPORT_INFO_PREFIX).forEach(info::put);
        return info;
    }

    private static Optional<String> version(Class<?> type) {
        return Optional.ofNullable(type.getPackage()).map(Package::getImplementationVersion);
    }

    private static Optional<String> hostName() {
        try {
            return Optional.of(InetAddress.getLocalHost().getHostName());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> ci() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("GITHUB_ACTIONS")) {
            return Optional.of("GitHub Actions · " + env.getOrDefault("GITHUB_REPOSITORY", "") + " #"
                    + env.getOrDefault("GITHUB_RUN_NUMBER", ""));
        }
        if (env.containsKey("GITLAB_CI")) {
            return Optional.of("GitLab CI · pipeline " + env.getOrDefault("CI_PIPELINE_ID", ""));
        }
        if (env.containsKey("JENKINS_URL")) {
            return Optional.of("Jenkins · " + env.getOrDefault("JOB_NAME", "") + " #" + env.getOrDefault("BUILD_NUMBER", ""));
        }
        if (env.containsKey("TF_BUILD")) {
            return Optional.of("Azure Pipelines · build " + env.getOrDefault("BUILD_BUILDNUMBER", ""));
        }
        return env.containsKey("CI") ? Optional.of("CI") : Optional.empty();
    }

    private static Optional<String> resource(String name) {
        try (InputStream in = ExtentReportManager.class.getClassLoader().getResourceAsStream(name)) {
            return in == null ? Optional.empty() : Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
