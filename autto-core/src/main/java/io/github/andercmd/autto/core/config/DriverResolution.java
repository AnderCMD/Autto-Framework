package io.github.andercmd.autto.core.config;

/** Strategy used to obtain the driver binary (chromedriver, geckodriver, msedgedriver) for local browsers. */
public enum DriverResolution {
    /**
     * WebDriverManager: detects the installed browser, downloads the matching driver, caches it and works behind
     * proxies and mirrors. Falls back to Selenium Manager when it fails (see {@code autto.driver.fallback}).
     */
    WEBDRIVERMANAGER,
    /** Selenium Manager (built into Selenium): also able to download Chrome/Firefox when they are missing. */
    SELENIUM_MANAGER
}
