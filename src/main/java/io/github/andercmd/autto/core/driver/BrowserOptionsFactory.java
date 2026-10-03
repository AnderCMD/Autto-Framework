package io.github.andercmd.autto.core.driver;

import io.github.andercmd.autto.core.config.AuttoConfig;
import io.github.andercmd.autto.core.config.ConfigKeys;
import io.github.andercmd.autto.core.media.EvidenceMode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Builds the browser specific {@code *Options} object from the framework configuration. */
public final class BrowserOptionsFactory {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserOptionsFactory.class);

    private BrowserOptionsFactory() {
    }

    public static AbstractDriverOptions<?> create(BrowserType browser, AuttoConfig config) {
        AbstractDriverOptions<?> options = switch (browser) {
            case CHROME -> chromium(new ChromeOptions(), config);
            case EDGE -> chromium(new EdgeOptions(), config);
            case FIREFOX -> firefox(config);
            case SAFARI -> safari(config);
        };
        common(options, config);
        return options;
    }

    // ------------------------------------------------------------------ per browser

    private static <T extends ChromiumOptions<T>> T chromium(T options, AuttoConfig config) {
        boolean headless = config.getBoolean(ConfigKeys.BROWSER_HEADLESS, false);
        if (headless) {
            options.addArguments("--headless=new");
        }
        if (config.getBoolean(ConfigKeys.BROWSER_INCOGNITO, false)) {
            options.addArguments(options instanceof EdgeOptions ? "--inprivate" : "--incognito");
        }
        options.addArguments("--disable-search-engine-choice-screen", "--no-first-run", "--no-default-browser-check");
        if (isLinuxContainer()) {
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        }
        windowSize(config).ifHeadlessOrFixed(headless, size -> options.addArguments("--window-size=" + size));
        config.find(ConfigKeys.BROWSER_BINARY).ifPresent(options::setBinary);
        config.find(ConfigKeys.BROWSER_MOBILE_EMULATION)
                .ifPresent(device -> options.setExperimentalOption("mobileEmulation", Map.of("deviceName", device)));

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir(config));
        prefs.put("download.prompt_for_download", false);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        config.withPrefix(ConfigKeys.BROWSER_PREFS_PREFIX).forEach((k, v) -> prefs.put(k, CapabilitiesParser.convert(v)));
        options.setExperimentalOption("prefs", prefs);
        options.addArguments(config.getList(ConfigKeys.BROWSER_ARGS));
        return options;
    }

    private static FirefoxOptions firefox(AuttoConfig config) {
        FirefoxOptions options = new FirefoxOptions();
        boolean headless = config.getBoolean(ConfigKeys.BROWSER_HEADLESS, false);
        if (headless) {
            options.addArguments("-headless");
        }
        if (config.getBoolean(ConfigKeys.BROWSER_INCOGNITO, false)) {
            options.addArguments("-private");
        }
        windowSize(config).ifHeadlessOrFixed(headless, size -> {
            String[] parts = size.split(",");
            options.addArguments("--width=" + parts[0], "--height=" + parts[1]);
        });
        config.find(ConfigKeys.BROWSER_BINARY).ifPresent(options::setBinary);

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.dir", downloadDir(config));
        profile.setPreference("browser.download.useDownloadDir", true);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/pdf,application/octet-stream,text/csv,application/zip,application/json");
        profile.setPreference("pdfjs.disabled", true);
        config.withPrefix(ConfigKeys.BROWSER_PREFS_PREFIX).forEach((k, v) -> {
            Object value = CapabilitiesParser.convert(v);
            switch (value) {
                case Boolean b -> profile.setPreference(k, b);
                case Integer i -> profile.setPreference(k, i);
                default -> profile.setPreference(k, String.valueOf(value));
            }
        });
        options.setProfile(profile);
        options.addArguments(config.getList(ConfigKeys.BROWSER_ARGS));
        return options;
    }

    private static SafariOptions safari(AuttoConfig config) {
        if (config.getBoolean(ConfigKeys.BROWSER_HEADLESS, false)) {
            LOG.warn("Safari does not support headless mode; the browser window will be visible.");
        }
        return new SafariOptions();
    }

    // ------------------------------------------------------------------ shared settings

    private static void common(AbstractDriverOptions<?> options, AuttoConfig config) {
        config.find(ConfigKeys.BROWSER_VERSION).ifPresent(options::setBrowserVersion);
        config.find(ConfigKeys.PLATFORM_NAME).ifPresent(options::setPlatformName);
        options.setAcceptInsecureCerts(config.getBoolean(ConfigKeys.BROWSER_ACCEPT_INSECURE_CERTS, true));
        options.setPageLoadStrategy(config.getEnum(ConfigKeys.BROWSER_PAGE_LOAD_STRATEGY, PageLoadStrategy.class,
                PageLoadStrategy.NORMAL));
        unhandledPromptBehaviour(config).ifPresent(options::setUnhandledPromptBehaviour);
        if (config.getBoolean(ConfigKeys.BROWSER_CONSOLE_LOGS, false) && !(options instanceof SafariOptions)) {
            options.setCapability("webSocketUrl", true);
        }
        applyCapabilities(options, config);
    }

    /** Copies every {@code capabilities.*} entry into the options object (vendor options included). */
    static void applyCapabilities(MutableCapabilities options, AuttoConfig config) {
        CapabilitiesParser.parse(config.withPrefix(ConfigKeys.CAPABILITIES_PREFIX)).forEach(options::setCapability);
    }

    /**
     * When video recording is enabled the recorder takes screenshots in the background. With the W3C default
     * ("dismiss and notify") a screenshot taken while an alert is open would dismiss it, so we switch to "ignore".
     */
    private static Optional<UnexpectedAlertBehaviour> unhandledPromptBehaviour(AuttoConfig config) {
        Optional<String> configured = config.find(ConfigKeys.BROWSER_UNHANDLED_PROMPT);
        if (configured.isPresent()) {
            String value = configured.get().trim().toLowerCase(Locale.ROOT).replace('_', ' ');
            UnexpectedAlertBehaviour behaviour = UnexpectedAlertBehaviour.fromString(value);
            if (behaviour == null) {
                throw new IllegalArgumentException("Invalid " + ConfigKeys.BROWSER_UNHANDLED_PROMPT + " '" + value
                        + "'. Use: accept, dismiss, accept and notify, dismiss and notify, ignore");
            }
            return Optional.of(behaviour);
        }
        if (config.getEnum(ConfigKeys.VIDEO_MODE, EvidenceMode.class, EvidenceMode.ON_FAILURE) != EvidenceMode.OFF) {
            return Optional.of(UnexpectedAlertBehaviour.IGNORE);
        }
        return Optional.empty();
    }

    private static String downloadDir(AuttoConfig config) {
        return Path.of(config.get(ConfigKeys.BROWSER_DOWNLOAD_DIR, "target/downloads")).toAbsolutePath().toString();
    }

    /** Chromium refuses to start as root or inside most containers unless its sandbox is disabled. */
    private static boolean isLinuxContainer() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")
                && (System.getenv("CI") != null
                        || Path.of("/.dockerenv").toFile().exists()
                        || "root".equals(System.getProperty("user.name")));
    }

    static WindowSize windowSize(AuttoConfig config) {
        return WindowSize.parse(config.get(ConfigKeys.BROWSER_WINDOW_SIZE, "1920x1080"));
    }

    /** Window size requested by configuration: {@code maximized} or {@code <width>x<height>}. */
    public record WindowSize(boolean maximized, int width, int height) {

        static WindowSize parse(String raw) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (value.equals("maximized") || value.equals("max")) {
                return new WindowSize(true, 1920, 1080);
            }
            List<String> parts = List.of(value.split("[x,]"));
            if (parts.size() != 2) {
                throw new IllegalArgumentException("Invalid " + ConfigKeys.BROWSER_WINDOW_SIZE + " '" + raw
                        + "'. Use 'maximized' or '<width>x<height>' (e.g. 1920x1080)");
            }
            return new WindowSize(false, Integer.parseInt(parts.get(0).trim()), Integer.parseInt(parts.get(1).trim()));
        }

        /** Headless browsers have no screen to maximize to, so a fixed size is always passed at start-up. */
        void ifHeadlessOrFixed(boolean headless, Consumer<String> consumer) {
            if (headless || !maximized) {
                consumer.accept(width + "," + height);
            }
        }
    }
}
