package io.github.andercmd.autto.core.driver;

/** Where the browser (or device) is started. */
public enum ExecutionTarget {
    /** Browser installed on the current machine. Drivers are resolved automatically by Selenium Manager. */
    LOCAL,
    /** Selenium Grid, Docker, or any cloud vendor exposing a W3C WebDriver endpoint (BrowserStack, Sauce Labs, LambdaTest...). */
    REMOTE,
    /** Real devices or emulators/simulators through an Appium 2+ server (Android / iOS). */
    APPIUM
}
