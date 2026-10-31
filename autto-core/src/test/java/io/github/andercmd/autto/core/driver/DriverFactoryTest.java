package io.github.andercmd.autto.core.driver;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.config.BrowserType;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;

class DriverFactoryTest {

    @TempDir
    Path temp;

    private AuttoSettings settings(String... keyValues) {
        Properties system = new Properties();
        system.setProperty("user.dir", temp.toString());
        for (int i = 0; i < keyValues.length; i += 2) {
            system.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return AuttoSettings.load(Map.of(), system);
    }

    /** Regression: options after a WebDriverManager resolution used to throw "Browser version must be set". */
    @ParameterizedTest
    @EnumSource(value = BrowserType.class, names = {"CHROME", "CHROMIUM", "FIREFOX", "EDGE"})
    void localOptionsAfterWebDriverManagerResolution(BrowserType browser) {
        DriverResolver.Resolution resolution =
                new DriverResolver.Resolution(DriverResolver.Strategy.WEBDRIVERMANAGER, Optional.empty());

        AbstractDriverOptions<?> options = DriverFactory.localOptions(browser, settings(), resolution);

        assertThat(options.getBrowserVersion()).isNullOrEmpty();
    }

    @Test
    void requestedVersionIsKeptForSeleniumManager() {
        DriverResolver.Resolution resolution =
                new DriverResolver.Resolution(DriverResolver.Strategy.SELENIUM_MANAGER, Optional.empty());

        AbstractDriverOptions<?> options = DriverFactory.localOptions(BrowserType.CHROME,
                settings("autto.browser.version", "beta"), resolution);

        assertThat(options.getBrowserVersion()).isEqualTo("beta");
    }

    @Test
    void chromiumUsesTheBinaryDetectedByWebDriverManager() {
        Path binary = temp.resolve("chromium");
        DriverResolver.Resolution resolution =
                new DriverResolver.Resolution(DriverResolver.Strategy.WEBDRIVERMANAGER, Optional.of(binary));

        AbstractDriverOptions<?> options = DriverFactory.localOptions(BrowserType.CHROMIUM, settings(), resolution);

        @SuppressWarnings("unchecked")
        Map<String, Object> chrome = (Map<String, Object>) options.asMap().get(ChromeOptions.CAPABILITY);
        assertThat(chrome).containsEntry("binary", binary.toFile().getPath());
    }

    @org.junit.jupiter.api.Test
    void detectsDriverAndBrowserVersionMismatch() {
        var mismatch = new org.openqa.selenium.SessionNotCreatedException(
                "session not created: This version of ChromeDriver only supports Chrome version 150\n"
                        + "Current browser version is 154.0.8037.97");

        org.assertj.core.api.Assertions.assertThat(DriverFactory.isVersionMismatch(mismatch)).isTrue();
        org.assertj.core.api.Assertions.assertThat(DriverFactory.isVersionMismatch(
                new org.openqa.selenium.SessionNotCreatedException("Could not start a new session. grid busy")))
                .isFalse();
    }
}
