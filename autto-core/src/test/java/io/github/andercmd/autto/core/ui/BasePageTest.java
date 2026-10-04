package io.github.andercmd.autto.core.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.driver.DriverManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class BasePageTest {

    private static final By FIELD = By.id("field");

    private static final Map<String, String> PROPERTIES = Map.of(
            "autto.timeouts.explicit", "1s",
            "autto.timeouts.polling", "50ms",
            "autto.base-url", "https://shop.test/");

    private WebDriver driver;
    private TestPage page;

    /** Exposes the protected API. */
    static class TestPage extends BasePage {
        void typeInto(By by, String text) {
            type(by, text);
        }

        String textOf(By by) {
            return text(by);
        }

        void open(String path, boolean ignored) {
            open(path);
        }

        void clickOn(By by) {
            click(by);
        }

        boolean displayed(By by) {
            return isDisplayed(by);
        }

        int total(By by) {
            return count(by);
        }

        <T> T retry(java.util.function.Supplier<T> action) {
            return retryStale(action);
        }
    }

    @BeforeEach
    void setUp() {
        // BasePage reads the process-wide settings
        PROPERTIES.forEach(System::setProperty);
        AuttoSettings.reset();
        driver = mock(WebDriver.class);
        DriverManager.adopt(driver, AuttoSettings.get());
        page = new TestPage();
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        PROPERTIES.keySet().forEach(System::clearProperty);
        AuttoSettings.reset();
    }

    private WebElement visibleElement(By by) {
        WebElement element = mock(WebElement.class);
        when(element.isDisplayed()).thenReturn(true);
        when(element.isEnabled()).thenReturn(true);
        when(driver.findElement(by)).thenReturn(element);
        return element;
    }

    @Test
    void typeClearsTheFieldAndSendsKeys() {
        WebElement element = visibleElement(FIELD);

        page.typeInto(FIELD, "hello");

        verify(element).clear();
        verify(element).sendKeys("hello");
    }

    @Test
    void typeOfEmptyTextOnlyClears() {
        WebElement element = visibleElement(FIELD);

        page.typeInto(FIELD, "");

        verify(element).clear();
        verify(element, never()).sendKeys(any(CharSequence[].class));
    }

    @Test
    void clickRetriesWhileTheElementIsStale() {
        WebElement element = mock(WebElement.class);
        when(driver.findElement(FIELD)).thenThrow(new StaleElementReferenceException("stale")).thenReturn(element);

        page.clickOn(FIELD);

        verify(element).click();
    }

    @Test
    void textSurvivesAReRender() {
        WebElement stale = mock(WebElement.class);
        when(stale.isDisplayed()).thenThrow(new StaleElementReferenceException("stale"));
        WebElement fresh = visibleElement(By.id("other"));
        when(fresh.getText()).thenReturn("  $39.98 ");
        when(driver.findElement(FIELD)).thenReturn(stale).thenReturn(fresh);

        assertThat(page.textOf(FIELD)).isEqualTo("$39.98");
    }

    @Test
    void retryStaleGivesUpAfterThreeAttempts() {
        int[] calls = {0};

        assertThatThrownBy(() -> page.retry(() -> {
            calls[0]++;
            throw new StaleElementReferenceException("stale");
        })).isInstanceOf(StaleElementReferenceException.class);
        assertThat(calls[0]).isEqualTo(3);
    }

    @Test
    void openJoinsTheBaseUrlWithoutDoubleSlashes() {
        page.open("/cart", true);
        page.open("https://other.test/x", true);

        verify(driver).get("https://shop.test/cart");
        verify(driver).get("https://other.test/x");
    }

    @Test
    void stateChecksNeverThrow() {
        when(driver.findElement(FIELD)).thenThrow(new NoSuchElementException("missing"));
        when(driver.findElements(FIELD)).thenReturn(List.of());

        assertThat(page.displayed(FIELD)).isFalse();
        assertThat(page.total(FIELD)).isZero();
    }

    @Test
    void clickTimesOutWhenTheElementNeverAppears() {
        when(driver.findElement(FIELD)).thenThrow(new StaleElementReferenceException("stale"));

        assertThatThrownBy(() -> page.clickOn(FIELD)).isInstanceOf(TimeoutException.class);
    }
}
