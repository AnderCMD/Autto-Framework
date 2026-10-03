package io.github.andercmd.autto.core.driver;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.github.andercmd.autto.core.config.AuttoConfig;
import io.github.andercmd.autto.core.config.ConfigKeys;
import java.net.URI;
import java.net.MalformedURLException;
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
 * Creates fully configured {@link WebDriver} instances for every supported {@link ExecutionTarget}.
 *
 * <p>Local drivers (chromedriver, geckodriver, msedgedriver) and even browsers are resolved automatically by
 * <a href="https://www.selenium.dev/documentation/selenium_manager/">Selenium Manager</a>, so nothing has to be
 * installed or downloaded by hand.
 */
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver create(AuttoConfig config) {
        ExecutionTarget target = config.getEnum(ConfigKeys.EXECUTION_TARGET, ExecutionTarget.class, ExecutionTarget.LOCAL);
        WebDriver driver = switch (target) {
            case LOCAL -> local(config);
            case REMOTE -> remote(config);
            case APPIUM -> appium(config);
        };
        configure(driver, config, target);
        return driver;
    }

    private static WebDriver local(AuttoConfig config) {
        BrowserType browser = BrowserType.from(config.get(ConfigKeys.BROWSER, "chrome"));
        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(browser, config);
        LOG.info("Starting local {} browser", browser.name().toLowerCase(Locale.ROOT));
        return switch (browser) {
            case CHROME -> new ChromeDriver((ChromeOptions) options);
            case FIREFOX -> new FirefoxDriver((FirefoxOptions) options);
            case EDGE -> new EdgeDriver((EdgeOptions) options);
            case SAFARI -> new SafariDriver((SafariOptions) options);
        };
    }

    private static WebDriver remote(AuttoConfig config) {
        BrowserType browser = BrowserType.from(config.get(ConfigKeys.BROWSER, "chrome"));
        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(browser, config);
        URL url = url(config.get(ConfigKeys.REMOTE_URL, "http://localhost:4444"), ConfigKeys.REMOTE_URL);
        LOG.info("Starting remote {} session on {}", browser.name().toLowerCase(Locale.ROOT), redact(url));
        RemoteWebDriver driver = (RemoteWebDriver) RemoteWebDriver.builder().oneOf(options).address(url).build();
        // Augmenting adds optional interfaces (BiDi, DevTools, ...) supported by the remote end.
        return new Augmenter().augment(driver);
    }

    private static WebDriver appium(AuttoConfig config) {
        String platform = config.get(ConfigKeys.APPIUM_PLATFORM, "android").toLowerCase(Locale.ROOT);
        URL url = url(config.get(ConfigKeys.APPIUM_URL, "http://127.0.0.1:4723"), ConfigKeys.APPIUM_URL);
        MutableCapabilities capabilities = new MutableCapabilities();
        BrowserOptionsFactory.applyCapabilities(capabilities, config);
        config.find(ConfigKeys.APPIUM_APP).ifPresent(app -> capabilities.setCapability("appium:app", app));
        LOG.info("Starting Appium {} session on {}", platform, redact(url));
        return switch (platform) {
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
                    "Unsupported " + ConfigKeys.APPIUM_PLATFORM + " '" + platform + "'. Use android or ios");
        };
    }

    private static void configure(WebDriver driver, AuttoConfig config, ExecutionTarget target) {
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.implicitlyWait(config.getSeconds(ConfigKeys.TIMEOUT_IMPLICIT, 0));
        if (target == ExecutionTarget.APPIUM) {
            return;
        }
        timeouts.pageLoadTimeout(config.getSeconds(ConfigKeys.TIMEOUT_PAGE_LOAD, 60));
        timeouts.scriptTimeout(config.getSeconds(ConfigKeys.TIMEOUT_SCRIPT, 30));

        if (config.find(ConfigKeys.BROWSER_MOBILE_EMULATION).isPresent()) {
            return;
        }
        BrowserOptionsFactory.WindowSize size = BrowserOptionsFactory.windowSize(config);
        try {
            if (size.maximized() && !config.getBoolean(ConfigKeys.BROWSER_HEADLESS, false)) {
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
            throw new IllegalArgumentException("Invalid URL in '" + key + "': " + value, e);
        }
    }

    /** Removes credentials (user:key@host) so they never end up in logs or reports. */
    public static String redact(URL url) {
        return redact(url.toString());
    }

    public static String redact(String url) {
        return url.replaceAll("(?<=://)[^/@]+@", "***@");
    }
}
