package io.github.andercmd.autto.core.config;

/** Where the browser (or device) is started. */
public enum ExecutionTarget {
    /** Browser installed on the current machine. Drivers are resolved by WebDriverManager / Selenium Manager. */
    LOCAL,
    /** Browser started in a disposable Docker container by WebDriverManager. Only Docker is required. */
    DOCKER,
    /** Selenium Grid or a cloud vendor exposing a W3C endpoint (BrowserStack, Sauce Labs, LambdaTest...). */
    REMOTE,
    /** Real devices or emulators/simulators through an Appium 2+ server (Android / iOS). */
    APPIUM
}
