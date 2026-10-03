package io.github.andercmd.autto.core.driver;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.config.BrowserType;
import io.github.andercmd.autto.core.config.EvidenceMode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.MutableCapabilities;
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

    public static AbstractDriverOptions<?> create(BrowserType browser, AuttoSettings settings) {
        AuttoProperties.Browser config = settings.properties().browser();
        AbstractDriverOptions<?> options = switch (browser) {
            case CHROME, CHROMIUM -> chromium(new ChromeOptions(), config, settings.browserPrefs());
            case EDGE -> chromium(new EdgeOptions(), config, settings.browserPrefs());
            case FIREFOX -> firefox(config, settings.browserPrefs());
            case SAFARI -> safari(config);
        };
        common(options, settings);
        return options;
    }

    // ------------------------------------------------------------------ per browser

    private static <T extends ChromiumOptions<T>> T chromium(T options, AuttoProperties.Browser config,
            Map<String, String> customPrefs) {
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        if (config.incognito()) {
            options.addArguments(options instanceof EdgeOptions ? "--inprivate" : "--incognito");
        }
        options.addArguments("--disable-search-engine-choice-screen", "--no-first-run", "--no-default-browser-check");
        if (needsNoSandbox()) {
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        }
        WindowSize.parse(config.windowSize())
                .ifFixed(config.headless(), (w, h) -> options.addArguments("--window-size=" + w + "," + h));
        if (config.binary() != null) {
            options.setBinary(config.binary());
        }
        if (config.mobileEmulation() != null) {
            options.setExperimentalOption("mobileEmulation", Map.of("deviceName", config.mobileEmulation()));
        }

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", absolute(config.downloadDir()));
        prefs.put("download.prompt_for_download", false);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        customPrefs.forEach((k, v) -> prefs.put(k, CapabilitiesParser.convert(v)));
        options.setExperimentalOption("prefs", prefs);
        options.addArguments(config.args());
        return options;
    }

    private static FirefoxOptions firefox(AuttoProperties.Browser config, Map<String, String> customPrefs) {
        FirefoxOptions options = new FirefoxOptions();
        if (config.headless()) {
            options.addArguments("-headless");
        }
        if (config.incognito()) {
            options.addArguments("-private");
        }
        WindowSize.parse(config.windowSize())
                .ifFixed(config.headless(), (w, h) -> options.addArguments("--width=" + w, "--height=" + h));
        if (config.binary() != null) {
            options.setBinary(config.binary());
        }
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.dir", absolute(config.downloadDir()));
        profile.setPreference("browser.download.useDownloadDir", true);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/pdf,application/octet-stream,text/csv,application/zip,application/json");
        profile.setPreference("pdfjs.disabled", true);
        customPrefs.forEach((k, v) -> {
            switch (CapabilitiesParser.convert(v)) {
                case Boolean b -> profile.setPreference(k, b);
                case Integer i -> profile.setPreference(k, i);
                case Object other -> profile.setPreference(k, String.valueOf(other));
            }
        });
        options.setProfile(profile);
        options.addArguments(config.args());
        return options;
    }

    private static SafariOptions safari(AuttoProperties.Browser config) {
        if (config.headless()) {
            LOG.warn("Safari does not support headless mode; the browser window will be visible.");
        }
        return new SafariOptions();
    }

    // ------------------------------------------------------------------ shared settings

    private static void common(AbstractDriverOptions<?> options, AuttoSettings settings) {
        AuttoProperties props = settings.properties();
        if (props.browser().version() != null) {
            options.setBrowserVersion(props.browser().version());
        }
        if (props.execution().platformName() != null) {
            options.setPlatformName(props.execution().platformName());
        }
        options.setAcceptInsecureCerts(props.browser().acceptInsecureCerts());
        options.setPageLoadStrategy(props.browser().pageLoadStrategy());
        unhandledPromptBehaviour(props).ifPresent(options::setUnhandledPromptBehaviour);
        if (props.browser().consoleLogs() && !(options instanceof SafariOptions)) {
            options.setCapability("webSocketUrl", true);
        }
        applyCapabilities(options, settings.capabilities());
    }

    /** Copies W3C / vendor capabilities into the options object. */
    static void applyCapabilities(MutableCapabilities options, Map<String, String> capabilities) {
        CapabilitiesParser.parse(capabilities).forEach(options::setCapability);
    }

    /**
     * When video recording is enabled the recorder takes screenshots in the background. With the W3C default
     * ("dismiss and notify") a screenshot taken while an alert is open would dismiss it, so we switch to "ignore".
     */
    static Optional<UnexpectedAlertBehaviour> unhandledPromptBehaviour(AuttoProperties props) {
        String configured = props.browser().unhandledPrompt();
        if (configured != null) {
            String value = configured.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
            UnexpectedAlertBehaviour behaviour = UnexpectedAlertBehaviour.fromString(value);
            if (behaviour == null) {
                throw new IllegalArgumentException("Invalid autto.browser.unhandled-prompt '" + configured
                        + "'. Use: accept, dismiss, accept and notify, dismiss and notify, ignore");
            }
            return Optional.of(behaviour);
        }
        return props.evidence().video() != EvidenceMode.OFF ? Optional.of(UnexpectedAlertBehaviour.IGNORE)
                : Optional.empty();
    }

    private static String absolute(String path) {
        return Path.of(path).toAbsolutePath().toString();
    }

    /** Chromium refuses to start as root or inside most containers unless its sandbox is disabled. */
    private static boolean needsNoSandbox() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")
                && (System.getenv("CI") != null
                        || Path.of("/.dockerenv").toFile().exists()
                        || "root".equals(System.getProperty("user.name")));
    }

    /** Window size requested by configuration: {@code maximized} or {@code <width>x<height>}. */
    public record WindowSize(boolean maximized, int width, int height) {

        public static WindowSize parse(String raw) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (value.equals("maximized") || value.equals("max")) {
                return new WindowSize(true, 1920, 1080);
            }
            List<String> parts = List.of(value.split("[x,]"));
            if (parts.size() != 2) {
                throw new IllegalArgumentException("Invalid autto.browser.window-size '" + raw
                        + "'. Use 'maximized' or '<width>x<height>' (e.g. 1920x1080)");
            }
            return new WindowSize(false, Integer.parseInt(parts.get(0).trim()), Integer.parseInt(parts.get(1).trim()));
        }

        /** Headless browsers have no screen to maximize to, so a fixed size is always passed at start-up. */
        void ifFixed(boolean headless, SizeConsumer consumer) {
            if (headless || !maximized) {
                consumer.accept(width, height);
            }
        }

        /** Receives a width and a height. */
        @FunctionalInterface
        interface SizeConsumer {
            void accept(int width, int height);
        }
    }
}
