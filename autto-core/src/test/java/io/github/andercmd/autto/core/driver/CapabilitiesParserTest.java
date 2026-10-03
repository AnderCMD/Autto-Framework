package io.github.andercmd.autto.core.driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CapabilitiesParserTest {

    @Test
    void buildsNestedVendorOptionsWithTypedValues() {
        Map<String, String> flat = new LinkedHashMap<>();
        flat.put("se:recordVideo", "true");
        flat.put("bstack:options.os", "Windows");
        flat.put("bstack:options.osVersion", "\"11\"");
        flat.put("bstack:options.idleTimeout", "300");
        flat.put("goog:chromeOptions.args", "[--lang=es, --mute-audio]");

        Map<String, Object> caps = CapabilitiesParser.parse(flat);

        assertThat(caps).containsEntry("se:recordVideo", true);
        assertThat(caps.get("bstack:options"))
                .isEqualTo(Map.of("os", "Windows", "osVersion", "11", "idleTimeout", 300));
        assertThat(caps.get("goog:chromeOptions")).isEqualTo(Map.of("args", List.of("--lang=es", "--mute-audio")));
    }

    @Test
    void yamlListIndexesBecomeLists() {
        Map<String, String> flat = new LinkedHashMap<>();
        flat.put("goog:chromeOptions.args[1]", "--mute-audio");
        flat.put("goog:chromeOptions.args[0]", "--lang=es");
        assertThat(CapabilitiesParser.parse(flat).get("goog:chromeOptions"))
                .isEqualTo(Map.of("args", List.of("--lang=es", "--mute-audio")));
    }

    @Test
    void rejectsConflictingKeys() {
        Map<String, String> flat = new LinkedHashMap<>();
        flat.put("a", "1");
        flat.put("a.b", "2");
        assertThatThrownBy(() -> CapabilitiesParser.parse(flat)).hasMessageContaining("conflicts");
    }

    @Test
    void redactsCredentialsFromUrls() {
        assertThat(DriverFactory.redact("https://user:key@hub.example.com/wd/hub"))
                .isEqualTo("https://***@hub.example.com/wd/hub");
        assertThat(DriverFactory.redact("http://localhost:4444")).isEqualTo("http://localhost:4444");
    }
}
