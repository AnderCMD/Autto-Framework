package io.github.andercmd.autto.core.cucumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.cucumber.java.Scenario;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.db.Database;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.network.NetworkMock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

class HooksTest {

    private static final Map<String, String> PROPERTIES = Map.of("autto.scenario.timeout", "150ms");

    private WebDriver driver;
    private Scenario scenario;

    @BeforeEach
    void setUp() {
        PROPERTIES.forEach(System::setProperty);
        AuttoSettings.reset();
        driver = mock(WebDriver.class);
        WebDriver.Options options = mock(WebDriver.Options.class);
        when(driver.manage()).thenReturn(options);
        when(options.window()).thenReturn(mock(WebDriver.Window.class));
        scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn("a scenario");
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        Thread.interrupted();
        PROPERTIES.keySet().forEach(System::clearProperty);
        AuttoSettings.reset();
    }

    @Test
    void timeoutHooksAbortAScenarioThatRunsTooLong() {
        CountDownLatch quit = new CountDownLatch(1);
        doAnswer(invocation -> {
            quit.countDown();
            return null;
        }).when(driver).quit();
        DriverManager.adopt(driver, AuttoSettings.get());
        ScenarioTimeoutHooks hooks = new ScenarioTimeoutHooks();

        hooks.arm(scenario);
        boolean interrupted = false;
        try {
            assertThat(quit.await(5, TimeUnit.SECONDS)).as("the browser was closed").isTrue();
        } catch (InterruptedException e) {
            interrupted = true; // the watchdog interrupts right after closing the browser
        }

        assertThat(interrupted || waitForInterrupt()).as("the worker thread is interrupted").isTrue();
        Thread.currentThread().interrupt();
        hooks.disarm();
        assertThat(Thread.currentThread().isInterrupted()).as("interrupt is not leaked").isFalse();
    }

    @Test
    void timeoutHooksDoNothingWhenTheScenarioFinishesInTime() {
        System.setProperty("autto.scenario.timeout", "10s");
        AuttoSettings.reset();
        ScenarioTimeoutHooks hooks = new ScenarioTimeoutHooks();

        hooks.arm(scenario);
        hooks.disarm();

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void viewportTagResizesTheWindow() {
        DriverManager.adopt(driver, AuttoSettings.get());
        when(scenario.getSourceTagNames()).thenReturn(List.of("@smoke", "@viewport:390x844"));

        new ScenarioTagHooks().applyTags(scenario);

        verify(driver.manage().window()).setSize(new Dimension(390, 844));
    }

    @Test
    void softAssertionHooksReportEveryFailure() {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(1).isEqualTo(2);
        softly.assertThat("a").isEqualTo("b");

        assertThatThrownBy(() -> new SoftAssertionHooks(softly).verifySoftAssertions())
                .isInstanceOf(AssertionError.class).hasMessageContaining("2 failures");
    }

    @Test
    void resourceHooksRunTheDatabaseCleanup() {
        Database database = mock(Database.class);
        NetworkMock network = mock(NetworkMock.class);

        new ScenarioResourceHooks(database, network).release();

        verify(network).close();
        verify(database).close();
        verify(scenario, org.mockito.Mockito.never()).log(any());
    }

    private static boolean waitForInterrupt() {
        try {
            java.util.concurrent.TimeUnit.SECONDS.sleep(2);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }
}
