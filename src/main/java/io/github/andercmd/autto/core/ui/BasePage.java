package io.github.andercmd.autto.core.ui;

import io.github.andercmd.autto.core.config.AuttoConfig;
import io.github.andercmd.autto.core.config.ConfigKeys;
import io.github.andercmd.autto.core.driver.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for page objects (and page components).
 *
 * <p>Design rules:
 *
 * <ul>
 *   <li>Locators are {@link By} constants, never {@code @FindBy} proxies, so pages stay thread-safe and stateless.
 *   <li>Every interaction waits explicitly for the right condition; implicit waits are disabled by default.
 *   <li>The driver is always looked up from {@link DriverManager}, so pages can be created by dependency injection
 *       before the browser exists and still work in parallel runs.
 * </ul>
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected WebDriver driver() {
        return DriverManager.driver();
    }

    protected AuttoConfig config() {
        return AuttoConfig.get();
    }

    // ------------------------------------------------------------------ navigation

    /** Opens a path relative to {@code base.url} (or an absolute URL). */
    protected void open(String pathOrUrl) {
        String url = pathOrUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*") ? pathOrUrl
                : config().require(ConfigKeys.BASE_URL).replaceAll("/+$", "") + "/" + pathOrUrl.replaceAll("^/+", "");
        log.info("Opening {}", url);
        driver().get(url);
    }

    public String currentUrl() {
        return driver().getCurrentUrl();
    }

    public String title() {
        return driver().getTitle();
    }

    // ------------------------------------------------------------------ waits

    protected WebDriverWait waiter() {
        return waiter(config().getSeconds(ConfigKeys.TIMEOUT_EXPLICIT, 15));
    }

    protected WebDriverWait waiter(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver(), timeout,
                Duration.ofMillis(config().getInt(ConfigKeys.TIMEOUT_POLLING_MS, 250)));
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    protected <T> T waitUntil(Function<WebDriver, T> condition) {
        return waiter().until(condition);
    }

    protected WebElement visible(By locator) {
        return waitUntil(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement clickable(By locator) {
        return waitUntil(ExpectedConditions.elementToBeClickable(locator));
    }

    protected List<WebElement> allVisible(By locator) {
        return waitUntil(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected boolean waitForInvisibility(By locator) {
        return waitUntil(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ------------------------------------------------------------------ interactions

    protected void click(By locator) {
        log.debug("Click {}", locator);
        waitUntil(d -> {
            try {
                d.findElement(locator).click();
                return true;
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                return false;
            }
        });
    }

    protected void type(By locator, String text) {
        log.debug("Type into {}", locator);
        WebElement element = visible(locator);
        element.clear();
        if (text != null && !text.isEmpty()) {
            element.sendKeys(text);
        }
    }

    protected String text(By locator) {
        return visible(locator).getText().trim();
    }

    protected List<String> texts(By locator) {
        return driver().findElements(locator).stream().map(WebElement::getText).map(String::trim).toList();
    }

    protected String attribute(By locator, String name) {
        return visible(locator).getDomAttribute(name);
    }

    protected void selectByValue(By locator, String value) {
        new Select(visible(locator)).selectByValue(value);
    }

    protected void selectByText(By locator, String text) {
        new Select(visible(locator)).selectByVisibleText(text);
    }

    protected void hover(By locator) {
        new Actions(driver()).moveToElement(visible(locator)).perform();
    }

    protected void scrollIntoView(By locator) {
        js("arguments[0].scrollIntoView({block: 'center'});", driver().findElement(locator));
    }

    protected Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver()).executeScript(script, args);
    }

    // ------------------------------------------------------------------ state checks (never throw)

    protected boolean isDisplayed(By locator) {
        try {
            return driver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    protected boolean isDisplayed(By locator, Duration timeout) {
        try {
            waiter(timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected int count(By locator) {
        return driver().findElements(locator).size();
    }
}
