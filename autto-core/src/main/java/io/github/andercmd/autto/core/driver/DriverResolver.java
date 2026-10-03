package io.github.andercmd.autto.core.driver;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.BrowserType;
import io.github.andercmd.autto.core.config.DriverResolution;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the driver binary of a local browser.
 *
 * <ol>
 *   <li><b>WebDriverManager</b> (default): detects the installed browser version, downloads the matching driver,
 *       caches it and honours proxies/mirrors.
 *   <li><b>Selenium Manager</b>: used when configured, when a specific browser version/channel is requested
 *       (Selenium Manager can download browsers) or as automatic fallback when WebDriverManager fails.
 * </ol>
 */
public final class DriverResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DriverResolver.class);
    /** WebDriverManager resolves each browser once per JVM; parallel scenarios reuse the result. */
    private static final Map<BrowserType, Resolution> RESOLVED = new ConcurrentHashMap<>();

    /** Decision taken for a browser. */
    public enum Strategy {
        WEBDRIVERMANAGER,
        SELENIUM_MANAGER,
        NONE
    }

    /** Outcome of the resolution: the strategy used and, for Chromium, the detected browser binary. */
    public record Resolution(Strategy strategy, Optional<Path> browserPath) {
    }

    private DriverResolver() {
    }

    /** Pure decision logic, without side effects. */
    public static Strategy strategyFor(BrowserType browser, AuttoProperties props) {
        if (browser == BrowserType.SAFARI) {
            return Strategy.NONE;
        }
        if (props.driver().resolution() == DriverResolution.SELENIUM_MANAGER || props.browser().version() != null) {
            return Strategy.SELENIUM_MANAGER;
        }
        return Strategy.WEBDRIVERMANAGER;
    }

    public static Resolution resolve(BrowserType browser, AuttoProperties props) {
        Strategy strategy = strategyFor(browser, props);
        if (strategy != Strategy.WEBDRIVERMANAGER) {
            LOG.debug("Driver for {} resolved by {}", browser, strategy);
            return new Resolution(strategy, Optional.empty());
        }
        return RESOLVED.computeIfAbsent(browser, b -> resolveWithWebDriverManager(b, props));
    }

    /**
     * Forgets what was resolved for the browser and resolves it again ignoring WebDriverManager's resolution cache.
     * Used when the browser auto-updated after the driver was chosen (driver and browser versions no longer match).
     */
    public static Resolution refresh(BrowserType browser, AuttoProperties props) {
        RESOLVED.remove(browser);
        if (strategyFor(browser, props) != Strategy.WEBDRIVERMANAGER) {
            return resolve(browser, props);
        }
        try {
            WebDriverManager.getInstance(browser.driverManagerType().orElseThrow()).clearResolutionCache();
        } catch (RuntimeException e) {
            LOG.debug("Resolution cache could not be cleared: {}", e.getMessage());
        }
        return RESOLVED.computeIfAbsent(browser, b -> resolveWithWebDriverManager(b, props, true));
    }

    private static Resolution resolveWithWebDriverManager(BrowserType browser, AuttoProperties props) {
        return resolveWithWebDriverManager(browser, props, false);
    }

    private static Resolution resolveWithWebDriverManager(BrowserType browser, AuttoProperties props,
            boolean fresh) {
        DriverManagerType type = browser.driverManagerType().orElseThrow();
        try {
            WebDriverManager wdm = WebDriverManager.getInstance(type);
            if (fresh) {
                wdm.avoidResolutionCache();
            }
            if (props.driver().cachePath() != null) {
                wdm.cachePath(props.driver().cachePath());
            }
            wdm.setup();
            Optional<Path> browserPath = browser == BrowserType.CHROMIUM ? wdm.getBrowserPath() : Optional.empty();
            LOG.info("Driver for {} resolved by WebDriverManager", browser);
            return new Resolution(Strategy.WEBDRIVERMANAGER, browserPath);
        } catch (RuntimeException e) {
            if (!props.driver().fallback()) {
                throw new IllegalStateException("WebDriverManager could not resolve the driver for " + browser
                        + " and autto.driver.fallback is disabled", e);
            }
            LOG.warn("WebDriverManager could not resolve the driver for {} ({}). Falling back to Selenium Manager.",
                    browser, e.getMessage());
            return new Resolution(Strategy.SELENIUM_MANAGER, Optional.empty());
        }
    }

    /** Whether the browser is installed on this machine (as detected by WebDriverManager). */
    public static boolean isInstalled(BrowserType browser) {
        if (browser == BrowserType.SAFARI) {
            return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
        }
        try {
            return WebDriverManager.getInstance(browser.driverManagerType().orElseThrow()).getBrowserPath().isPresent();
        } catch (RuntimeException e) {
            return true; // unknown: let the normal start-up report the real problem
        }
    }
}
