package io.github.andercmd.autto.core.driver;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andercmd.autto.core.config.AuttoConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.CapabilityType;

class BrowserOptionsFactoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void chromeHeadlessWithCustomCapabilities() {
        AuttoConfig config = AuttoConfig.of(Map.of(
                "browser.headless", "true",
                "browser.version", "stable",
                "browser.args", "--lang=es",
                "browser.window.size", "1366x768",
                "platform.name", "linux",
                "capabilities.se:name", "Autto"));

        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(BrowserType.CHROME, config);

        assertThat(options).isInstanceOf(ChromeOptions.class);
        Map<String, Object> chrome = (Map<String, Object>) options.asMap().get(ChromeOptions.CAPABILITY);
        assertThat((List<String>) chrome.get("args")).contains("--headless=new", "--lang=es", "--window-size=1366,768");
        assertThat(options.getBrowserVersion()).isEqualTo("stable");
        assertThat(options.getCapability("se:name")).isEqualTo("Autto");
        assertThat(options.getCapability(CapabilityType.PLATFORM_NAME)).hasToString("linux");
    }

    @Test
    void videoRecordingSwitchesAlertsToIgnore() {
        AbstractDriverOptions<?> options = BrowserOptionsFactory.create(BrowserType.FIREFOX,
                AuttoConfig.of(Map.of("video.mode", "on_failure")));
        assertThat(options).isInstanceOf(FirefoxOptions.class);
        assertThat(options.getCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR))
                .isEqualTo(UnexpectedAlertBehaviour.IGNORE);

        AbstractDriverOptions<?> noVideo = BrowserOptionsFactory.create(BrowserType.FIREFOX,
                AuttoConfig.of(Map.of("video.mode", "off")));
        assertThat(noVideo.getCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR)).isNull();
    }

    @Test
    void windowSizeParsing() {
        assertThat(BrowserOptionsFactory.WindowSize.parse("maximized").maximized()).isTrue();
        assertThat(BrowserOptionsFactory.WindowSize.parse("1280x720"))
                .isEqualTo(new BrowserOptionsFactory.WindowSize(false, 1280, 720));
    }
}
