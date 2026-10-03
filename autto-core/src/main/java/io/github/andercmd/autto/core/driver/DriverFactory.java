package io.github.andercmd.autto.core.driver;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.config.BrowserType;
import io.github.andercmd.autto.core.config.ExecutionTarget;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates fully configured drivers for every {@link ExecutionTarget}.
 *
 * <p>For local runs the driver binary is resolved by {@link DriverResolver} (WebDriverManager with Selenium Manager
 * as fallback). When {@code autto.driver.docker-fallback=true} and the requested browser is not installed, the
 * browser is started in Docker instead, so the suite runs "with any browser, no matter what".
 */
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static DriverHandle create(AuttoSettings settings) {
        AuttoProperties props = settings.properties();
        ExecutionTarget target = props.execution().target();
        BrowserType browser = props.browser().name();
        if (target == ExecutionTarget.LOCAL && props.driver().dockerFallback()
                && !DriverResolver.isInstalled(browser)) {
            if (WebDriverManager.isDockerAvailable()) {
                LOG.warn("{} is not installed locally: starting it in Docker (autto.driver.docker-fallback)", browser);
                target = ExecutionTarget.DOCKER;
            } else {
                LOG.warn("{} is not installed and Docker is not available; trying a local start anyway", browser);
            }
        }
        DriverHandle handle = switch (target) {
            case LOCAL -> local(browser, settings);
            case DOCKER -> docker(browser, settings);
            case REMOTE -> remote(browser, settings);
            case APPIUM -> appium(settings);
        };
        configure(handle.driver(), props, target);
        return handle;
    }

    private static DriverHandle local(BrowserType browser, AuttoSettings settings) {
        DriverResolver.Resolution resolution = DriverResolver.resolve(browser, settings.properties());
        AbstractDriverOptions<?> options = localOptions(browser, settings, resolution);
        String name = browser.name().toLowerCase(Locale.ROOT);
        LOG.info("Starting local {} (driver: {})", name, resolution.strategy().name().toLowerCase(Locale.ROOT));
        WebDriver driver = switch (browser) {
            case CHROME, CHROMIUM -> new ChromeDriver((ChromeOptions) options);
            case FIREFOX -> new FirefoxDriver((FirefoxOptions) options);
            case EDGE -> new EdgeDriver((EdgeOptions) options);
            case SAFARI -> new SafariDriver((SafariOptions) options);
        };
        return DriverHandle.of(driver, "local " + name);
    }

    /**
     * Options of a local browser. When WebDriverManager resolved the driver no browser version is requested (the
     * driver matches the installed browser); a requested version always goes through Selenium Manager.
     */
    static AbstractDriverOptions<?> localOptions(BrowserType browser, AuttoSettings settings,
            DriverResolver.Resolution resolution) {
        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(browser, settings);
        if (browser == BrowserType.CHROMIUM && settings.properties().browser().binary() == null) {
            resolution.browserPath().ifPresent(path -> ((ChromeOptions) options).setBinary(path.toFile()));
        }
        return options;
    }

    private static DriverHandle docker(BrowserType browser, AuttoSettings settings) {
        if (browser == BrowserType.SAFARI) {
            throw new IllegalArgumentException("Safari cannot run in Docker; use a macOS machine or a cloud provider");
        }
        AuttoProperties props = settings.properties();
        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(browser, settings);
        WebDriverManager wdm = WebDriverManager.getInstance(browser.driverManagerType().orElseThrow())
                .browserInDocker()
                .dockerScreenResolution(props.docker().screenResolution())
                .dockerShmSize(props.docker().shmSize())
                .capabilities(options);
        if (props.browser().version() != null) {
            wdm.browserVersion(props.browser().version());
        }
        if (props.docker().vnc()) {
            wdm.enableVnc();
        }
        String name = browser.name().toLowerCase(Locale.ROOT);
        LOG.info("Starting {} in Docker", name);
        WebDriver driver = wdm.create();
        if (props.docker().vnc()) {
            LOG.info("Watch the browser live: {}", wdm.getDockerNoVncUrl(driver));
        }
        return new DriverHandle(driver, "docker " + name, () -> wdm.quit(driver));
    }

    private static DriverHandle remote(BrowserType browser, AuttoSettings settings) {
        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(browser, settings);
        URL url = url(settings.properties().execution().remoteUrl(), "autto.execution.remote-url");
        LOG.info("Starting remote {} session on {}", browser.name().toLowerCase(Locale.ROOT), redact(url.toString()));
        WebDriver driver = new Augmenter().augment(RemoteWebDriver.builder().oneOf(options).address(url).build());
        return DriverHandle.of(driver, "remote " + browser.name().toLowerCase(Locale.ROOT));
    }

    private static DriverHandle appium(AuttoSettings settings) {
        AuttoProperties.Appium config = settings.properties().appium();
        String platform = config.platform().toLowerCase(Locale.ROOT);
        URL url = url(config.url(), "autto.appium.url");
        MutableCapabilities capabilities = new MutableCapabilities();
        BrowserOptionsFactory.applyCapabilities(capabilities, settings.capabilities());
        if (config.app() != null) {
            capabilities.setCapability("appium:app", config.app());
        }
        LOG.info("Starting Appium {} session on {}", platform, redact(url.toString()));
        WebDriver driver = switch (platform) {
            case "android" -> {
                capabilities.setCapability("platformName", "Android");
                if (capabilities.getCapability("appium:automationName") == null) {
                    capabilities.setCapability("appium:automationName", "UiAutomator2");
                }
                yield new AndroidDriver(url, capabilities);
            }
            case "ios" -> {
                capabilities.setCapability("platformName", "iOS");
                if (capabilities.getCapability("appium:automationName") == null) {
                    capabilities.setCapability("appium:automationName", "XCUITest");
                }
                yield new IOSDriver(url, capabilities);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported autto.appium.platform '" + platform + "'. Use android or ios");
        };
        return DriverHandle.of(driver, "appium " + platform);
    }

    private static void configure(WebDriver driver, AuttoProperties props, ExecutionTarget target) {
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.implicitlyWait(props.timeouts().implicit());
        if (target == ExecutionTarget.APPIUM) {
            return;
        }
        timeouts.pageLoadTimeout(props.timeouts().pageLoad());
        timeouts.scriptTimeout(props.timeouts().script());
        if (props.browser().mobileEmulation() != null) {
            return;
        }
        BrowserOptionsFactory.WindowSize size = BrowserOptionsFactory.WindowSize.parse(props.browser().windowSize());
        try {
            if (size.maximized() && !props.browser().headless()) {
                driver.manage().window().maximize();
            } else {
                driver.manage().window().setSize(new Dimension(size.width(), size.height()));
            }
        } catch (RuntimeException e) {
            LOG.debug("Window could not be resized: {}", e.getMessage());
        }
    }

    private static URL url(String value, String key) {
        try {
            return new URI(value).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL in '" + key + "': " + redact(value), e);
        }
    }

    /** Removes credentials (user:key@host) so they never end up in logs or reports. */
    public static String redact(String url) {
        return url.replaceAll("(?<=://)[^/@]+@", "***@");
    }
}
