package io.github.andercmd.autto.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AllureResultTest {

    @Test
    void mapsCucumberStatusesToAllure() {
        assertThat(AllureResult.status("PASSED", null)).isEqualTo("passed");
        assertThat(AllureResult.status("FAILED", new AssertionError("x"))).isEqualTo("failed");
        assertThat(AllureResult.status("FAILED", new IllegalStateException("x"))).isEqualTo("broken");
        assertThat(AllureResult.status("SKIPPED", null)).isEqualTo("skipped");
        assertThat(AllureResult.status("UNDEFINED", null)).isEqualTo("skipped");
    }

    @Test
    void serialisesStepsLabelsAndAttachments() throws Exception {
        AllureResult result = new AllureResult("u-1", "Login", "Feature: Login", 1000);
        result.label("tag", "smoke");
        AllureResult.Step step = new AllureResult.Step("Given a user", 1000);
        step.stop = 1500;
        step.attachments.add(java.util.Map.of("name", "shot", "source", "a.png", "type", "image/png"));
        result.steps.add(step);
        result.status = "failed";
        result.message = "boom";
        result.stop = 2000;

        JsonNode json = new ObjectMapper().readTree(result.toJson());

        assertThat(json.get("uuid").asText()).isEqualTo("u-1");
        assertThat(json.get("status").asText()).isEqualTo("failed");
        assertThat(json.get("statusDetails").get("message").asText()).isEqualTo("boom");
        assertThat(json.get("labels").get(0).get("value").asText()).isEqualTo("smoke");
        assertThat(json.get("steps").get(0).get("attachments").get(0).get("source").asText()).isEqualTo("a.png");
        assertThat(json.get("stop").asLong() - json.get("start").asLong()).isEqualTo(1000);
    }
}
