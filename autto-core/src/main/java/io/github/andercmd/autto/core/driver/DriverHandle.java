package io.github.andercmd.autto.core.driver;

import org.openqa.selenium.WebDriver;

/**
 * A started driver plus the action that releases it (quit the browser, stop its Docker container...).
 *
 * @param driver the WebDriver
 * @param description how it was started, for logs ("local chrome", "docker firefox", ...)
 * @param closer releases every resource of the session
 */
public record DriverHandle(WebDriver driver, String description, Runnable closer) {

    public static DriverHandle of(WebDriver driver, String description) {
        return new DriverHandle(driver, description, driver::quit);
    }
}
