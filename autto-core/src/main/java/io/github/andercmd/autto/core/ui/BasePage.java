package io.github.andercmd.autto.core.ui;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.driver.DriverManager;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
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
 * <p>Annotate concrete pages with {@link PageObject} so Spring creates one instance per scenario and injects it
 * into step definitions.
 *
 * <ul>
 *   <li>Locators are {@link By} constants, never {@code @FindBy} proxies, so pages stay thread-safe and stateless.
 *   <li>Every interaction waits explicitly for the right condition; implicit waits are disabled by default.
 *   <li>The driver is always looked up from {@link DriverManager}, so pages can be created by dependency injection
 *       before the browser exists and still work in parallel runs.
 *   <li>Page objects never assert: they expose state, steps decide.
 * </ul>
 */
public abstract class BasePage {

    private static final int STALE_RETRIES = 3;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected WebDriver driver() {
        return DriverManager.driver();
    }

    protected AuttoProperties config() {
        return AuttoSettings.get().properties();
    }

    // ------------------------------------------------------------------ navigation

    /** Opens a path relative to {@code autto.base-url} (or an absolute URL). */
    protected void open(String pathOrUrl) {
        String url = pathOrUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*") ? pathOrUrl
                : baseUrl().replaceAll("/+$", "") + "/" + pathOrUrl.replaceAll("^/+", "");
        log.info("Opening {}", url);
        driver().get(url);
    }

    private String baseUrl() {
        String baseUrl = config().baseUrl();
        if (baseUrl == null) {
            throw new IllegalStateException("autto.base-url is not configured (application.yml or AUTTO_BASE_URL)");
        }
        return baseUrl;
    }

    public String currentUrl() {
        return driver().getCurrentUrl();
    }

    public String title() {
        return driver().getTitle();
    }

    /** Waits until the document is fully loaded ({@code document.readyState == 'complete'}). */
    protected void waitForPageLoad() {
        waitUntil(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    protected void waitForUrlContains(String fragment) {
        waitUntil(ExpectedConditions.urlContains(fragment));
    }

    // ------------------------------------------------------------------ waits

    protected WebDriverWait waiter() {
        return waiter(config().timeouts().explicit());
    }

    protected WebDriverWait waiter(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver(), timeout,
                config().timeouts().polling());
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

    /** Waits until the element contains {@code text} (e.g. a status that changes asynchronously). */
    protected void waitForText(By locator, String text) {
        waitUntil(ExpectedConditions.textToBePresentInElementLocated(locator, text));
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
        retryStale(() -> {
            WebElement element = visible(locator);
            element.clear();
            if (text != null && !text.isEmpty()) {
                element.sendKeys(text);
            }
            return null;
        });
    }

    protected String text(By locator) {
        return retryStale(() -> visible(locator).getText().trim());
    }

    protected List<String> texts(By locator) {
        return retryStale(() -> driver().findElements(locator).stream().map(WebElement::getText).map(String::trim)
                .toList());
    }

    protected String attribute(By locator, String name) {
        return retryStale(() -> visible(locator).getDomAttribute(name));
    }

    protected void selectByValue(By locator, String value) {
        retryStale(() -> {
            new Select(visible(locator)).selectByValue(value);
            return null;
        });
    }

    protected void selectByText(By locator, String text) {
        retryStale(() -> {
            new Select(visible(locator)).selectByVisibleText(text);
            return null;
        });
    }

    /** Clicks through JavaScript. Last resort for elements covered by overlays that a user could still click. */
    protected void jsClick(By locator) {
        log.debug("JavaScript click {}", locator);
        retryStale(() -> js("arguments[0].click();", visible(locator)));
    }

    protected void doubleClick(By locator) {
        retryStale(() -> {
            new Actions(driver()).doubleClick(clickable(locator)).perform();
            return null;
        });
    }

    protected void pressKeys(By locator, CharSequence... keys) {
        retryStale(() -> {
            visible(locator).sendKeys(keys);
            return null;
        });
    }

    /** Types the text and presses Enter (search boxes, single-field forms). */
    protected void typeAndSubmit(By locator, String text) {
        type(locator, text);
        pressKeys(locator, Keys.ENTER);
    }

    /** Selects a local file in an {@code <input type=file>} (works on local, Grid and cloud browsers). */
    protected void upload(By fileInput, Path file) {
        driver().findElement(fileInput).sendKeys(file.toAbsolutePath().toString());
    }

    protected void hover(By locator) {
        retryStale(() -> {
            new Actions(driver()).moveToElement(visible(locator)).perform();
            return null;
        });
    }

    protected void scrollIntoView(By locator) {
        js("arguments[0].scrollIntoView({block: 'center'});", driver().findElement(locator));
    }

    /**
     * Runs an interaction again (up to {@value #STALE_RETRIES} times) when the page re-renders between locating the
     * element and using it. Single-page applications do this constantly; it is the main source of flaky UI tests.
     */
    protected <T> T retryStale(Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                if (attempt >= STALE_RETRIES) {
                    throw e;
                }
                log.debug("Element went stale, retrying ({}/{})", attempt, STALE_RETRIES);
            }
        }
    }

    protected Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver()).executeScript(script, args);
    }

    // ------------------------------------------------------------------ frames, windows and alerts

    protected void switchToFrame(By frame) {
        waitUntil(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frame));
    }

    protected void switchToDefaultContent() {
        driver().switchTo().defaultContent();
    }

    /**
     * Runs {@code action} (a click that opens a tab or window), waits for the new window and switches to it.
     *
     * @return handle of the original window, to come back with {@link #switchToWindow(String)}
     */
    protected String switchToNewWindow(Runnable action) {
        String original = driver().getWindowHandle();
        Set<String> before = driver().getWindowHandles();
        action.run();
        String opened = waitUntil(d -> d.getWindowHandles().stream().filter(h -> !before.contains(h)).findFirst()
                .orElse(null));
        driver().switchTo().window(opened);
        return original;
    }

    protected void switchToWindow(String handle) {
        driver().switchTo().window(handle);
    }

    /** Accepts the JavaScript alert/confirm and returns its text. */
    protected String acceptAlert() {
        Alert alert = waitUntil(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        alert.accept();
        return text;
    }

    /** Dismisses the JavaScript confirm and returns its text. */
    protected String dismissAlert() {
        Alert alert = waitUntil(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        alert.dismiss();
        return text;
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
