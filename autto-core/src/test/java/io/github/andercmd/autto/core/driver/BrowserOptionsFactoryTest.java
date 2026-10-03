package io.github.andercmd.autto.core.driver;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.config.BrowserType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.CapabilityType;

class BrowserOptionsFactoryTest {

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

    @Test
    @SuppressWarnings("unchecked")
    void chromeHeadlessWithCustomCapabilities() {
        AuttoSettings settings = settings(
                "autto.browser.headless", "true",
                "autto.browser.version", "stable",
                "autto.browser.window-size", "1366x768",
                "autto.execution.platform-name", "linux");

        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(BrowserType.CHROME, settings);

        assertThat(options).isInstanceOf(ChromeOptions.class);
        Map<String, Object> chrome = (Map<String, Object>) options.asMap().get(ChromeOptions.CAPABILITY);
        assertThat((List<String>) chrome.get("args"))
                .contains("--headless=new", "--lang=es", "--mute-audio", "--window-size=1366,768");
        assertThat(options.getBrowserVersion()).isEqualTo("stable");
        assertThat(options.getCapability("se:recordVideo")).isEqualTo(true);
        assertThat(options.getCapability("bstack:options")).isEqualTo(Map.of("os", "Windows", "osVersion", "11"));
        assertThat(options.getCapability(CapabilityType.PLATFORM_NAME)).hasToString("linux");
    }

    @Test
    void videoRecordingSwitchesAlertsToIgnore() {
        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(BrowserType.FIREFOX,
                settings("autto.evidence.video", "on-failure"));
        assertThat(options).isInstanceOf(FirefoxOptions.class);
        assertThat(options.getCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR))
                .isEqualTo(UnexpectedAlertBehaviour.IGNORE);

        AbstractDriverOptions<?> noVideo = BrowserOptionsFactory.create(BrowserType.FIREFOX,
                settings("autto.evidence.video", "off"));
        assertThat(noVideo.getCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR)).isNull();
    }

    @Test
    void windowSizeParsing() {
        assertThat(BrowserOptionsFactory.WindowSize.parse("maximized").maximized()).isTrue();
        assertThat(BrowserOptionsFactory.WindowSize.parse("1280x720"))
                .isEqualTo(new BrowserOptionsFactory.WindowSize(false, 1280, 720));
    }
}
