package io.github.andercmd.autto.core.config;

/** Every configuration key understood by the framework. See {@code docs/en/configuration.md}. */
public final class ConfigKeys {

    // ---- general
    public static final String ENV = "env";
    public static final String BASE_URL = "base.url";

    // ---- browser
    public static final String BROWSER = "browser";
    public static final String BROWSER_VERSION = "browser.version";
    public static final String BROWSER_HEADLESS = "browser.headless";
    public static final String BROWSER_WINDOW_SIZE = "browser.window.size";
    public static final String BROWSER_ARGS = "browser.args";
    public static final String BROWSER_BINARY = "browser.binary";
    public static final String BROWSER_INCOGNITO = "browser.incognito";
    public static final String BROWSER_MOBILE_EMULATION = "browser.mobile.emulation";
    public static final String BROWSER_ACCEPT_INSECURE_CERTS = "browser.accept.insecure.certs";
    public static final String BROWSER_PAGE_LOAD_STRATEGY = "browser.page.load.strategy";
    public static final String BROWSER_DOWNLOAD_DIR = "browser.download.dir";
    public static final String BROWSER_UNHANDLED_PROMPT = "browser.unhandled.prompt";
    public static final String BROWSER_CONSOLE_LOGS = "browser.console.logs";
    public static final String BROWSER_PREFS_PREFIX = "browser.prefs.";

    // ---- execution target
    public static final String EXECUTION_TARGET = "execution.target";
    public static final String REMOTE_URL = "remote.url";
    public static final String PLATFORM_NAME = "platform.name";
    public static final String CAPABILITIES_PREFIX = "capabilities.";
    public static final String APPIUM_URL = "appium.url";
    public static final String APPIUM_PLATFORM = "appium.platform";
    public static final String APPIUM_APP = "appium.app";

    // ---- timeouts (seconds)
    public static final String TIMEOUT_IMPLICIT = "timeouts.implicit";
    public static final String TIMEOUT_EXPLICIT = "timeouts.explicit";
    public static final String TIMEOUT_PAGE_LOAD = "timeouts.page.load";
    public static final String TIMEOUT_SCRIPT = "timeouts.script";
    public static final String TIMEOUT_POLLING_MS = "timeouts.polling.ms";

    // ---- evidence
    public static final String SCREENSHOT_MODE = "screenshot.mode";
    public static final String VIDEO_MODE = "video.mode";
    public static final String VIDEO_FPS = "video.fps";
    public static final String VIDEO_MAX_SECONDS = "video.max.seconds";
    public static final String VIDEO_MAX_WIDTH = "video.max.width";
    public static final String ATTACH_PAGE_SOURCE = "evidence.page.source";

    // ---- report
    public static final String REPORT_DIR = "report.dir";
    public static final String REPORT_TIMESTAMPED = "report.timestamped";
    public static final String REPORT_TITLE = "report.title";
    public static final String REPORT_NAME = "report.name";
    public static final String REPORT_THEME = "report.theme";
    public static final String REPORT_OFFLINE = "report.offline";
    public static final String REPORT_TIMELINE = "report.timeline";
    public static final String REPORT_BASE64_SCREENSHOTS = "report.screenshots.base64";
    public static final String REPORT_AUTHOR = "report.author";
    public static final String REPORT_SHOW_HOST = "report.show.host";
    public static final String REPORT_INFO_PREFIX = "report.info.";

    private ConfigKeys() {
    }
}
