package io.github.andercmd.autto.core.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One scenario in the Allure results format ({@code <uuid>-result.json}). */
final class AllureResult {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    final String uuid;
    final String name;
    final String fullName;
    final long start;
    final List<Map<String, String>> labels = new ArrayList<>();
    final List<Step> steps = new ArrayList<>();
    final List<Map<String, String>> attachments = new ArrayList<>();
    String status = "passed";
    String message;
    String trace;
    long stop;

    AllureResult(String uuid, String name, String fullName, long start) {
        this.uuid = uuid;
        this.name = name;
        this.fullName = fullName;
        this.start = start;
    }

    /** Allure statuses: assertion failures are {@code failed}, any other error is {@code broken}. */
    static String status(String cucumberStatus, Throwable error) {
        return switch (cucumberStatus.toUpperCase(Locale.ROOT)) {
            case "PASSED" -> "passed";
            case "SKIPPED", "PENDING", "UNUSED", "UNDEFINED" -> "skipped";
            default -> error instanceof AssertionError ? "failed" : "broken";
        };
    }

    void label(String name, String value) {
        labels.add(Map.of("name", name, "value", value));
    }

    String toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("uuid", uuid);
        json.put("historyId", Integer.toHexString(fullName.hashCode()));
        json.put("name", name);
        json.put("fullName", fullName);
        json.put("status", status);
        if (message != null) {
            Map<String, String> details = new LinkedHashMap<>();
            details.put("message", message);
            details.put("trace", trace == null ? "" : trace);
            json.put("statusDetails", details);
        }
        json.put("stage", "finished");
        json.put("start", start);
        json.put("stop", stop);
        json.put("labels", labels);
        json.put("steps", steps.stream().map(Step::toMap).toList());
        json.put("attachments", attachments);
        try {
            return MAPPER.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Allure result could not be serialised", e);
        }
    }

    /** A Gherkin step. */
    static final class Step {
        final String name;
        final long start;
        final List<Map<String, String>> attachments = new ArrayList<>();
        String status = "passed";
        String message;
        long stop;

        Step(String name, long start) {
            this.name = name;
            this.start = start;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", name);
            map.put("status", status);
            if (message != null) {
                map.put("statusDetails", Map.of("message", message));
            }
            map.put("stage", "finished");
            map.put("start", start);
            map.put("stop", stop);
            map.put("attachments", attachments);
            return map;
        }
    }
}
