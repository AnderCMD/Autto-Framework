package io.github.andercmd.autto.core.config;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.PageLoadStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed, validated configuration of the framework ({@code autto.*} in {@code application.yml}).
 *
 * <p>Every group has sensible defaults, so an empty configuration is valid. Free-form maps (W3C capabilities,
 * browser preferences, dashboard rows) are read by {@link AuttoSettings} because their keys may contain characters
 * such as {@code :} that are not valid in bound property names.
 *
 * @param baseUrl base URL of the application under test, used by {@code BasePage.open("/path")}
 * @param browser browser selection and options
 * @param driver how driver binaries are resolved
 * @param execution where the browser runs
 * @param docker options of {@code execution.target=docker}
 * @param appium options of {@code execution.target=appium}
 * @param timeouts WebDriver and wait timeouts
 * @param evidence screenshots, videos and page sources
 * @param report Extent report settings
 * @param api REST API client ({@code Api})
 * @param accessibility accessibility audits ({@code Accessibility})
 * @param scenario per-scenario guard rails (timeout)
 * @param performance web performance budgets ({@code WebPerformance})
 * @param visual visual regression ({@code VisualRegression})
 * @param db JDBC access for data set-up and verification ({@code Database})
 * @param mail test mailbox ({@code Mailbox})
 * @param notifications run summary sent to Slack, Teams or a generic webhook
 */
@ConfigurationProperties(prefix = "autto")
public record AuttoProperties(
        String baseUrl,
        Browser browser,
        Driver driver,
        Execution execution,
        Docker docker,
        Appium appium,
        Timeouts timeouts,
        Evidence evidence,
        Reporting report,
        Api api,
        Accessibility accessibility,
        Scenario scenario,
        Performance performance,
        Visual visual,
        Db db,
        Mail mail,
        Notifications notifications) {

    public AuttoProperties {
        baseUrl = blankToNull(baseUrl);
        browser = browser != null ? browser
                : new Browser(null, null, null, null, null, null, null, null, null, null, null, null, null);
        driver = driver != null ? driver : new Driver(null, null, null, null, null, null);
        execution = execution != null ? execution : new Execution(null, null, null);
        docker = docker != null ? docker : new Docker(null, null, null);
        appium = appium != null ? appium : new Appium(null, null, null);
        timeouts = timeouts != null ? timeouts : new Timeouts(null, null, null, null, null);
        evidence = evidence != null ? evidence : new Evidence(null, null, null, null, null, null);
        report = report != null ? report : new Reporting(null, null, null, null, null, null, null, null, null, null);
        api = api != null ? api : new Api(null, null, null, null, null);
        accessibility = accessibility != null ? accessibility : new Accessibility(null, null, null);
        scenario = scenario != null ? scenario : new Scenario(null);
        performance = performance != null ? performance : new Performance(null, null, null, null, null);
        visual = visual != null ? visual : new Visual(null, null, null, null, null);
        db = db != null ? db : new Db(null, null, null, null);
        mail = mail != null ? mail : new Mail(null, null);
        notifications = notifications != null ? notifications : new Notifications(null, null, null, null);
    }

    /** Configuration with every default value. */
    public static AuttoProperties defaults() {
        return new AuttoProperties(null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    /**
     * @param name browser to use
     * @param version exact version or channel (stable, beta, dev, canary); empty = installed browser
     * @param headless run without a visible window
     * @param windowSize "maximized" or "WIDTHxHEIGHT"
     * @param args extra command line arguments
     * @param binary custom browser executable
     * @param incognito private / incognito window
     * @param mobileEmulation Chromium device name to emulate (e.g. "iPhone 14 Pro Max")
     * @param acceptInsecureCerts accept self-signed certificates
     * @param pageLoadStrategy normal, eager or none
     * @param downloadDir download folder
     * @param unhandledPrompt accept, dismiss, accept and notify, dismiss and notify, ignore
     *     (empty = ignore while recording video)
     * @param consoleLogs collect browser console messages through WebDriver BiDi
     */
    public record Browser(
            BrowserType name,
            String version,
            Boolean headless,
            String windowSize,
            List<String> args,
            String binary,
            Boolean incognito,
            String mobileEmulation,
            Boolean acceptInsecureCerts,
            PageLoadStrategy pageLoadStrategy,
            String downloadDir,
            String unhandledPrompt,
            Boolean consoleLogs) {

        public Browser {
            name = name != null ? name : BrowserType.CHROME;
            version = blankToNull(version);
            headless = headless != null && headless;
            windowSize = blankToNull(windowSize) != null ? windowSize.trim() : "1920x1080";
            args = args != null ? args.stream().filter(a -> a != null && !a.isBlank()).toList() : List.of();
            binary = blankToNull(binary);
            incognito = incognito != null && incognito;
            mobileEmulation = blankToNull(mobileEmulation);
            acceptInsecureCerts = acceptInsecureCerts == null || acceptInsecureCerts;
            pageLoadStrategy = pageLoadStrategy != null ? pageLoadStrategy : PageLoadStrategy.NORMAL;
            downloadDir = blankToNull(downloadDir) != null ? downloadDir : "target/downloads";
            unhandledPrompt = blankToNull(unhandledPrompt);
            consoleLogs = consoleLogs == null || consoleLogs;
        }
    }

    /**
     * @param resolution webdrivermanager (default) or selenium-manager
     * @param fallback use Selenium Manager when WebDriverManager cannot resolve the driver
     * @param dockerFallback start the browser in Docker when it is not installed locally (requires Docker)
     * @param cachePath driver cache folder of WebDriverManager (default ~/.cache/selenium)
     * @param startRetries extra attempts when the browser session cannot be created (busy Grid, cloud queue...)
     * @param startRetryDelay pause between attempts (doubled after every failed attempt)
     */
    public record Driver(DriverResolution resolution, Boolean fallback, Boolean dockerFallback, String cachePath,
            Integer startRetries, Duration startRetryDelay) {

        public Driver {
            resolution = resolution != null ? resolution : DriverResolution.WEBDRIVERMANAGER;
            fallback = fallback == null || fallback;
            dockerFallback = dockerFallback != null && dockerFallback;
            cachePath = blankToNull(cachePath);
            startRetries = startRetries != null ? startRetries : 1;
            startRetryDelay = startRetryDelay != null ? startRetryDelay : Duration.ofSeconds(2);
            if (startRetries < 0 || startRetries > 10) {
                throw new IllegalArgumentException(
                        "autto.driver.start-retries must be between 0 and 10, was " + startRetries);
            }
            if (startRetryDelay.isNegative()) {
                throw new IllegalArgumentException("autto.driver.start-retry-delay must not be negative");
            }
        }
    }

    /**
     * @param target local, docker, remote or appium
     * @param remoteUrl Selenium Grid / cloud hub URL (credentials are redacted in logs and reports)
     * @param platformName requested operating system for remote sessions
     */
    public record Execution(ExecutionTarget target, String remoteUrl, String platformName) {

        public Execution {
            target = target != null ? target : ExecutionTarget.LOCAL;
            remoteUrl = blankToNull(remoteUrl) != null ? remoteUrl.trim() : "http://localhost:4444";
            platformName = blankToNull(platformName);
        }
    }

    /**
     * @param vnc expose a noVNC URL to watch the containerized browser
     * @param screenResolution virtual screen of the container
     * @param shmSize shared memory of the container
     */
    public record Docker(Boolean vnc, String screenResolution, String shmSize) {

        public Docker {
            vnc = vnc != null && vnc;
            screenResolution = blankToNull(screenResolution) != null ? screenResolution : "1920x1080x24";
            shmSize = blankToNull(shmSize) != null ? shmSize : "2g";
        }
    }

    /**
     * @param url Appium server URL
     * @param platform android or ios
     * @param app path or URL of the .apk / .ipa / .app (empty for mobile web)
     */
    public record Appium(String url, String platform, String app) {

        public Appium {
            url = blankToNull(url) != null ? url.trim() : "http://127.0.0.1:4723";
            platform = blankToNull(platform) != null ? platform.trim() : "android";
            app = blankToNull(app);
        }
    }

    /**
     * @param implicit implicit wait (keep it at 0: the framework uses explicit waits)
     * @param explicit default explicit wait of page objects
     * @param pageLoad page load timeout
     * @param script asynchronous script timeout
     * @param polling polling interval of explicit waits
     */
    public record Timeouts(Duration implicit, Duration explicit, Duration pageLoad, Duration script, Duration polling) {

        public Timeouts {
            implicit = implicit != null ? implicit : Duration.ZERO;
            explicit = explicit != null ? explicit : Duration.ofSeconds(15);
            pageLoad = pageLoad != null ? pageLoad : Duration.ofSeconds(60);
            script = script != null ? script : Duration.ofSeconds(30);
            polling = polling != null ? polling : Duration.ofMillis(250);
            requirePositive("autto.timeouts.explicit", explicit);
            requirePositive("autto.timeouts.polling", polling);
        }
    }

    /**
     * @param screenshot off, on-failure or always (after every step)
     * @param video off, on-failure (recorded always, kept for failures) or always
     * @param videoFps frames per second of the recording (1-30)
     * @param videoMaxDuration only the last part of long scenarios is kept
     * @param videoMaxWidth frames are downscaled to this width
     * @param pageSource attach the HTML of the page when a scenario fails
     */
    public record Evidence(EvidenceMode screenshot, EvidenceMode video, Integer videoFps, Duration videoMaxDuration,
            Integer videoMaxWidth, Boolean pageSource) {

        public Evidence {
            screenshot = screenshot != null ? screenshot : EvidenceMode.ON_FAILURE;
            video = video != null ? video : EvidenceMode.ON_FAILURE;
            videoFps = videoFps != null ? videoFps : 3;
            videoMaxDuration = videoMaxDuration != null ? videoMaxDuration : Duration.ofMinutes(5);
            videoMaxWidth = videoMaxWidth != null ? videoMaxWidth : 1280;
            pageSource = pageSource == null || pageSource;
            if (videoFps < 1 || videoFps > 30) {
                throw new IllegalArgumentException(
                        "autto.evidence.video-fps must be between 1 and 30, was " + videoFps);
            }
            requirePositive("autto.evidence.video-max-duration", videoMaxDuration);
        }
    }

    /**
     * @param dir output folder of the report and the evidence
     * @param timestamped one sub-folder per run (yyyyMMdd-HHmmss)
     * @param title browser tab title
     * @param name name shown in the header
     * @param theme dark or standard
     * @param offline copy the report assets so it opens without internet
     * @param timeline show the timeline chart
     * @param screenshotsBase64 embed screenshots in the HTML instead of PNG files
     * @param author default author when a scenario has no @author:name tag
     * @param showHost show user and host name in the dashboard
     */
    public record Reporting(String dir, Boolean timestamped, String title, String name, String theme, Boolean offline,
            Boolean timeline, Boolean screenshotsBase64, String author, Boolean showHost) {

        public Reporting {
            dir = blankToNull(dir) != null ? dir.trim() : "target/autto-reports";
            timestamped = timestamped != null && timestamped;
            title = blankToNull(title) != null ? title : "Autto · Test Automation Report";
            name = blankToNull(name) != null ? name : "Autto Framework · Execution report";
            theme = blankToNull(theme) != null ? theme.trim() : "dark";
            offline = offline == null || offline;
            timeline = timeline == null || timeline;
            screenshotsBase64 = screenshotsBase64 != null && screenshotsBase64;
            author = blankToNull(author);
            showHost = showHost == null || showHost;
        }
    }

    /**
     * @param baseUrl base URL of the API under test (empty = absolute URLs in every request)
     * @param connectTimeout TCP connection timeout
     * @param readTimeout socket read timeout
     * @param relaxedHttps trust any certificate and host name (test environments with self-signed certificates)
     * @param report attach every request and response (masked) to the report
     */
    public record Api(String baseUrl, Duration connectTimeout, Duration readTimeout, Boolean relaxedHttps,
            Boolean report) {

        public Api {
            baseUrl = blankToNull(baseUrl) != null ? baseUrl.trim() : null;
            connectTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(10);
            readTimeout = readTimeout != null ? readTimeout : Duration.ofSeconds(30);
            relaxedHttps = relaxedHttps != null && relaxedHttps;
            report = report == null || report;
            requirePositive("autto.api.connect-timeout", connectTimeout);
            requirePositive("autto.api.read-timeout", readTimeout);
        }
    }

    /**
     * @param tags axe-core rule tags to run (wcag2a, wcag2aa, wcag21aa, best-practice...)
     * @param disabledRules axe rule ids that are never evaluated
     * @param failOn minimum impact that {@code AccessibilityResult.assertNoViolations()} rejects
     */
    public record Accessibility(List<String> tags, List<String> disabledRules, Impact failOn) {

        public Accessibility {
            tags = tags != null && !tags.isEmpty() ? List.copyOf(tags) : List.of("wcag2a", "wcag2aa", "wcag21a",
                    "wcag21aa");
            disabledRules = disabledRules != null ? List.copyOf(disabledRules) : List.of();
            failOn = failOn != null ? failOn : Impact.SERIOUS;
        }
    }

    /**
     * @param timeout a scenario that runs longer is aborted (its browser is closed) so a hung scenario cannot block
     *     a whole parallel run; zero disables the guard
     */
    public record Scenario(Duration timeout) {

        public Scenario {
            timeout = timeout != null ? timeout : Duration.ofMinutes(10);
            if (timeout.isNegative()) {
                throw new IllegalArgumentException("autto.scenario.timeout must not be negative");
            }
        }
    }

    /**
     * Web performance budgets; a value of zero (or less) disables that budget.
     *
     * @param fcp maximum First Contentful Paint
     * @param lcp maximum Largest Contentful Paint (Chromium only)
     * @param cls maximum Cumulative Layout Shift (Chromium only)
     * @param ttfb maximum Time To First Byte
     * @param load maximum time until the load event
     */
    public record Performance(Duration fcp, Duration lcp, Double cls, Duration ttfb, Duration load) {

        public Performance {
            fcp = fcp != null ? fcp : Duration.ofMillis(1800);
            lcp = lcp != null ? lcp : Duration.ofMillis(2500);
            cls = cls != null ? cls : 0.1;
            ttfb = ttfb != null ? ttfb : Duration.ofMillis(800);
            load = load != null ? load : Duration.ofSeconds(5);
        }
    }

    /**
     * @param baselineDir folder with the approved baseline images
     * @param tolerance share of different pixels (0-1) still considered equal
     * @param pixelThreshold per-channel difference (0-255) below which two pixels are equal
     * @param update create or replace baselines instead of comparing (also {@code -Dautto.visual.update=true})
     * @param diffDir where actual and diff images of failed comparisons are written
     */
    public record Visual(String baselineDir, Double tolerance, Integer pixelThreshold, Boolean update,
            String diffDir) {

        public Visual {
            baselineDir = blankToNull(baselineDir) != null ? baselineDir.trim() : "src/test/resources/visual";
            tolerance = tolerance != null ? tolerance : 0.001;
            pixelThreshold = pixelThreshold != null ? pixelThreshold : 10;
            update = update != null && update;
            diffDir = blankToNull(diffDir) != null ? diffDir.trim() : "target/autto-reports/visual";
            if (tolerance < 0 || tolerance > 1) {
                throw new IllegalArgumentException("autto.visual.tolerance must be between 0 and 1, was " + tolerance);
            }
            if (pixelThreshold < 0 || pixelThreshold > 255) {
                throw new IllegalArgumentException(
                        "autto.visual.pixel-threshold must be between 0 and 255, was " + pixelThreshold);
            }
        }
    }

    /**
     * @param url JDBC URL (empty = database support disabled)
     * @param username database user
     * @param password database password (masked in logs and reports)
     * @param driverClass optional JDBC driver class, only for drivers that are not auto-registered
     */
    public record Db(String url, String username, String password, String driverClass) {

        public Db {
            url = blankToNull(url) != null ? url.trim() : null;
            username = blankToNull(username);
            password = blankToNull(password);
            driverClass = blankToNull(driverClass);
        }
    }

    /**
     * @param url base URL of the Mailpit / MailHog-compatible API
     * @param timeout how long to wait for a message
     */
    public record Mail(String url, Duration timeout) {

        public Mail {
            url = blankToNull(url) != null ? url.trim().replaceAll("/+$", "") : "http://localhost:8025";
            timeout = timeout != null ? timeout : Duration.ofSeconds(30);
            requirePositive("autto.mail.timeout", timeout);
        }
    }

    /**
     * @param webhookUrl incoming webhook (empty = notifications disabled; keep it in a secret)
     * @param type slack, teams or generic (plain JSON summary)
     * @param onlyOnFailure notify only when at least one scenario failed
     * @param reportUrl link to the published report, shown in the message (e.g. the CI run or GitHub Pages URL)
     */
    public record Notifications(String webhookUrl, String type, Boolean onlyOnFailure, String reportUrl) {

        public Notifications {
            webhookUrl = blankToNull(webhookUrl);
            type = blankToNull(type) != null ? type.trim().toLowerCase(java.util.Locale.ROOT) : "slack";
            onlyOnFailure = onlyOnFailure != null && onlyOnFailure;
            reportUrl = blankToNull(reportUrl);
            if (!List.of("slack", "teams", "generic").contains(type)) {
                throw new IllegalArgumentException(
                        "autto.notifications.type must be slack, teams or generic, was '" + type + "'");
            }
        }
    }

    /** Impact levels of accessibility violations, from lowest to highest. */
    public enum Impact {
        MINOR, MODERATE, SERIOUS, CRITICAL
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void requirePositive(String key, Duration value) {
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(key + " must be greater than zero, was " + value);
        }
    }
}
