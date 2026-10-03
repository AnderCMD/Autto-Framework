package io.github.andercmd.autto.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andercmd.autto.core.security.Secrets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuttoSettingsTest {

    @TempDir
    Path temp;

    @AfterEach
    void cleanSecrets() {
        Secrets.clear();
    }

    private Properties system() {
        Properties properties = new Properties();
        properties.setProperty("user.dir", temp.toString()); // isolate from any real .env file
        return properties;
    }

    @Test
    void readsYamlAndDefaultProfile() {
        AuttoSettings settings = AuttoSettings.load(Map.of(), system());
        AuttoProperties props = settings.properties();

        assertThat(settings.activeProfiles()).containsExactly("unit");
        assertThat(props.baseUrl()).isEqualTo("https://example.test");
        assertThat(props.browser().name()).isEqualTo(BrowserType.FIREFOX);
        assertThat(props.browser().args()).containsExactly("--lang=es", "--mute-audio");
        assertThat(props.timeouts().explicit()).isEqualTo(Duration.ofSeconds(20));
        assertThat(props.evidence().video()).isEqualTo(EvidenceMode.OFF);
        // untouched groups keep their defaults
        assertThat(props.driver().resolution()).isEqualTo(DriverResolution.WEBDRIVERMANAGER);
        assertThat(props.execution().target()).isEqualTo(ExecutionTarget.LOCAL);
    }

    @Test
    void precedenceIsSystemThenEnvThenDotEnvThenProfiles() throws Exception {
        Path dotEnv = temp.resolve(".env");
        Files.writeString(dotEnv, """
                # local developer settings
                AUTTO_BROWSER_NAME=edge
                AUTTO_BROWSER_WINDOW_SIZE=1280x720
                export SPRING_PROFILES_ACTIVE="unit,ci"
                SAUCE_PASSWORD='s3cr3t-value'
                """);
        Properties system = system();
        system.setProperty("autto.browser.name", "chrome");

        AuttoSettings settings = AuttoSettings.load(Map.of("AUTTO_BROWSER_WINDOW_SIZE", "800x600"), system);
        AuttoProperties props = settings.properties();

        assertThat(settings.dotEnvFile()).contains(dotEnv);
        assertThat(settings.activeProfiles()).as("profiles selected from .env").containsExactly("unit", "ci");
        assertThat(props.browser().name()).as("system property wins").isEqualTo(BrowserType.CHROME);
        assertThat(props.browser().windowSize()).as("env var beats .env").isEqualTo("800x600");
        assertThat(props.browser().headless()).as("from application-ci.yml").isTrue();
        assertThat(props.timeouts().explicit()).as("last profile wins").isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.find("SAUCE_PASSWORD")).contains("s3cr3t-value");
        assertThat(Secrets.mask("password is s3cr3t-value")).isEqualTo("password is ******");
    }

    @Test
    void freeFormMapsKeepTheirOriginalKeys() {
        AuttoSettings settings = AuttoSettings.load(Map.of(), system());
        assertThat(settings.capabilities())
                .containsEntry("se:recordVideo", "true")
                .containsEntry("appium:deviceName", "Pixel 8")
                .containsEntry("bstack:options.os", "Windows")
                .containsEntry("bstack:options.osVersion", "\"11\"");
        assertThat(settings.reportInfo()).containsEntry("Team", "QA");
    }

    @Test
    void invalidValuesFailFastWithTheKeyName() {
        Properties system = system();
        system.setProperty("autto.evidence.video-fps", "99");
        assertThatThrownBy(() -> AuttoSettings.load(Map.of(), system))
                .hasMessageContaining("Invalid Autto configuration")
                .hasMessageContaining("video-fps");

        Properties browser = system();
        browser.setProperty("autto.browser.name", "netscape");
        assertThatThrownBy(() -> AuttoSettings.load(Map.of(), browser)).hasMessageContaining("autto.browser.name");
    }

    @Test
    void explicitDotEnvPathMustExist() {
        Properties system = system();
        system.setProperty(DotEnv.PATH_PROPERTY, temp.resolve("missing.env").toString());
        assertThatThrownBy(() -> AuttoSettings.load(Map.of(), system)).hasMessageContaining("missing.env");
    }

    @Test
    void defaultsAreValid() {
        AuttoProperties defaults = AuttoProperties.defaults();
        assertThat(defaults.browser().name()).isEqualTo(BrowserType.CHROME);
        assertThat(defaults.browser().args()).isEqualTo(List.of());
        assertThat(defaults.evidence().videoFps()).isEqualTo(3);
        assertThat(defaults.report().dir()).isEqualTo("target/autto-reports");
    }
}
