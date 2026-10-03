package io.github.andercmd.autto.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andercmd.autto.core.driver.ExecutionTarget;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class AuttoConfigTest {

    @Test
    void layersAreAppliedInOrder() {
        Properties system = new Properties();
        system.setProperty("browser", "firefox");
        Map<String, String> env = Map.of("AUTTO_BROWSER_HEADLESS", "true", "AUTTO_BROWSER", "edge");

        AuttoConfig config = AuttoConfig.load(env, system);

        assertThat(config.get("browser", "chrome")).as("system property wins").isEqualTo("firefox");
        assertThat(config.getBoolean("browser.headless", false)).as("env var applied").isTrue();
        assertThat(config.get("base.url", "")).as("environment file loaded").isEqualTo("https://www.saucedemo.com");
        assertThat(config.get("env", "")).isEqualTo("qa");
    }

    @Test
    void blankValuesNeverOverrideLowerLayers() {
        Properties system = new Properties();
        system.setProperty("browser", "  ");
        AuttoConfig config = AuttoConfig.load(Map.of(), system);
        assertThat(config.get("browser", "none")).isEqualTo("chrome");
    }

    @Test
    void typedAccessors() {
        AuttoConfig config = AuttoConfig.of(Map.of(
                "flag", "yes",
                "number", "42",
                "seconds", "1.5",
                "list", "a, b,,c",
                "target", "remote"));

        assertThat(config.getBoolean("flag", false)).isTrue();
        assertThat(config.getInt("number", 0)).isEqualTo(42);
        assertThat(config.getSeconds("seconds", 0)).isEqualTo(Duration.ofMillis(1500));
        assertThat(config.getList("list")).containsExactly("a", "b", "c");
        assertThat(config.getEnum("target", ExecutionTarget.class, ExecutionTarget.LOCAL)).isEqualTo(ExecutionTarget.REMOTE);
        assertThat(config.getInt("missing", 7)).isEqualTo(7);
    }

    @Test
    void invalidValuesHaveHelpfulMessages() {
        AuttoConfig config = AuttoConfig.of(Map.of("flag", "maybe", "target", "moon"));
        assertThatThrownBy(() -> config.getBoolean("flag", false)).hasMessageContaining("flag").hasMessageContaining("boolean");
        assertThatThrownBy(() -> config.getEnum("target", ExecutionTarget.class, ExecutionTarget.LOCAL))
                .hasMessageContaining("local, remote, appium");
        assertThatThrownBy(() -> config.require("nope")).hasMessageContaining("AUTTO_NOPE");
    }

    @Test
    void placeholdersAreResolvedFromConfigAndEnvironment() {
        Properties system = new Properties();
        system.setProperty("remote.url", "https://${CLOUD_USER}:${cloud.key}@hub.example.com/${path:wd/hub}");
        system.setProperty("cloud.key", "s3cr3t");

        AuttoConfig config = AuttoConfig.load(Map.of("CLOUD_USER", "jane"), system);

        assertThat(config.get("remote.url", "")).isEqualTo("https://jane:s3cr3t@hub.example.com/wd/hub");
    }

    @Test
    void unresolvablePlaceholdersFailFast() {
        Properties system = new Properties();
        system.setProperty("remote.url", "${NOT_DEFINED_ANYWHERE}");
        AuttoConfig config = AuttoConfig.load(Map.of(), system);
        assertThatThrownBy(() -> config.find("remote.url")).hasMessageContaining("NOT_DEFINED_ANYWHERE");
    }

    @Test
    void prefixLookup() {
        AuttoConfig config = AuttoConfig.of(Map.of("capabilities.a", "1", "capabilities.b.c", "2", "other", "3"));
        assertThat(config.withPrefix("capabilities.")).containsOnly(Map.entry("a", "1"), Map.entry("b.c", "2"));
    }
}
