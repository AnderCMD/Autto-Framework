package io.github.andercmd.autto.core.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.JsonFormatter;
import com.aventstack.extentreports.reporter.configuration.ExtentSparkReporterConfig;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;
import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.config.ExecutionTarget;
import io.github.andercmd.autto.core.driver.DriverFactory;
import io.github.andercmd.autto.core.security.Secrets;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
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
            extent = create(AuttoSettings.get(), ReportPaths.get());
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

    private static ExtentReports create(AuttoSettings settings, ReportPaths paths) {
        AuttoProperties.Reporting config = settings.properties().report();
        ExtentReports reports = new ExtentReports();
        reports.setReportUsesManualConfiguration(true);

        ExtentSparkReporter full = spark(config, paths, "index.html", config.name());
        ExtentSparkReporter failed = spark(config, paths, "failed.html", "Failures only");
        failed.filter().statusFilter().as(new Status[] {Status.FAIL, Status.WARNING}).apply();
        JsonFormatter json = new JsonFormatter(paths.resolve("extent.json").toFile());
        reports.attachReporter(full, failed, json);

        environment(settings).forEach((key, value) -> reports.setSystemInfo(key, Secrets.mask(value)));
        LOG.info("Extent report will be written to {}", paths.resolve("index.html").toUri());
        return reports;
    }

    private static ExtentSparkReporter spark(AuttoProperties.Reporting config, ReportPaths paths, String file,
            String reportName) {
        ExtentSparkReporter spark = new ExtentSparkReporter(paths.resolve(file).toFile());
        spark.viewConfigurer().viewOrder().as(VIEW_ORDER).apply();
        ExtentSparkReporterConfig conf = spark.config();
        conf.setTheme("dark".equalsIgnoreCase(config.theme()) ? Theme.DARK : Theme.STANDARD);
        conf.setDocumentTitle(config.title());
        conf.setReportName(reportName);
        conf.setEncoding(StandardCharsets.UTF_8.name());
        conf.setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        conf.setTimelineEnabled(config.timeline());
        conf.enableOfflineMode(config.offline());
        conf.thumbnailForBase64(true);
        resource("autto/report/autto.css").ifPresent(conf::setCss);
        resource("autto/report/autto.js").ifPresent(conf::setJs);
        return spark;
    }

    static Map<String, String> environment(AuttoSettings settings) {
        AuttoProperties props = settings.properties();
        Map<String, String> info = new LinkedHashMap<>();
        ExecutionTarget target = props.execution().target();
        List<String> profiles = settings.activeProfiles();
        info.put("Profiles", profiles.isEmpty() ? "default" : String.join(", ", profiles));
        Optional.ofNullable(props.baseUrl()).ifPresent(url -> info.put("Base URL", url));
        info.put("Execution target", lower(target));
        if (target == ExecutionTarget.APPIUM) {
            info.put("Mobile platform", props.appium().platform());
            info.put("Appium server", DriverFactory.redact(props.appium().url()));
        } else {
            info.put("Browser", lower(props.browser().name())
                    + Optional.ofNullable(props.browser().version()).map(v -> " " + v).orElse(""));
            info.put("Headless", String.valueOf(props.browser().headless()));
            info.put("Driver resolution", lower(props.driver().resolution()));
            Optional.ofNullable(props.browser().mobileEmulation()).ifPresent(d -> info.put("Mobile emulation", d));
        }
        if (target == ExecutionTarget.REMOTE) {
            info.put("Remote URL", DriverFactory.redact(props.execution().remoteUrl()));
        }
        Optional.ofNullable(props.execution().platformName()).ifPresent(p -> info.put("Requested platform", p));
        info.put("Screenshots", lower(props.evidence().screenshot()));
        info.put("Videos", lower(props.evidence().video()));
        settings.find("cucumber.filter.tags").ifPresent(tags -> info.put("Tags", tags));
        info.put("Parallel", settings.find("cucumber.execution.parallel.enabled").orElse("false"));
        info.put("Operating system", System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");
        info.put("Java", System.getProperty("java.version") + " · " + System.getProperty("java.vendor"));
        version(org.openqa.selenium.WebDriver.class).ifPresent(v -> info.put("Selenium", v));
        version(io.cucumber.plugin.Plugin.class).ifPresent(v -> info.put("Cucumber", v));
        version(org.springframework.boot.SpringApplication.class).ifPresent(v -> info.put("Spring Boot", v));
        if (props.report().showHost()) {
            info.put("User", System.getProperty("user.name"));
            hostName().ifPresent(h -> info.put("Host", h));
        }
        ci().ifPresent(ci -> info.put("CI", ci));
        settings.reportInfo().forEach(info::put);
        return info;
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
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
            return Optional.of("Jenkins · " + env.getOrDefault("JOB_NAME", "") + " #"
                    + env.getOrDefault("BUILD_NUMBER", ""));
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
