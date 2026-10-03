package io.github.andercmd.autto.core.driver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Converts flat {@code capabilities.*} configuration entries into the nested structure expected by WebDriver.
 *
 * <pre>
 * capabilities.se:recordVideo=true            -&gt; {"se:recordVideo": true}
 * capabilities.bstack:options.os=Windows      -&gt; {"bstack:options": {"os": "Windows"}}
 * capabilities.bstack:options.osVersion=11    -&gt; {"bstack:options": {"os": "Windows", "osVersion": 11}}
 * capabilities.appium:deviceName=Pixel 8      -&gt; {"appium:deviceName": "Pixel 8"}
 * capabilities.goog:chromeOptions.args=[a,b]  -&gt; {"goog:chromeOptions": {"args": ["a", "b"]}}
 * </pre>
 *
 * <p>Values are converted to booleans, integers or decimals when they look like one. Wrap a value in quotes to keep
 * it as a string ({@code "11"}) and use {@code [a,b]} for lists.
 */
public final class CapabilitiesParser {

    private static final Pattern INTEGER = Pattern.compile("-?\\d{1,9}");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+\\.\\d+");

    private CapabilitiesParser() {
    }

    public static Map<String, Object> parse(Map<String, String> flat) {
        Map<String, Object> root = new LinkedHashMap<>();
        flat.forEach((key, value) -> put(root, key.split("\\."), convert(value)));
        return root;
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> root, String[] path, Object value) {
        Map<String, Object> current = root;
        for (int i = 0; i < path.length - 1; i++) {
            Object next = current.computeIfAbsent(path[i], k -> new LinkedHashMap<String, Object>());
            if (!(next instanceof Map)) {
                throw new IllegalArgumentException("Capability '" + String.join(".", path)
                        + "' conflicts with a scalar value already defined for '" + path[i] + "'");
            }
            current = (Map<String, Object>) next;
        }
        current.put(path[path.length - 1], value);
    }

    static Object convert(String raw) {
        String value = raw.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.startsWith("[") && value.endsWith("]")) {
            List<Object> list = new ArrayList<>();
            String body = value.substring(1, value.length() - 1).trim();
            if (!body.isEmpty()) {
                for (String item : body.split(",")) {
                    list.add(convert(item));
                }
            }
            return list;
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        if (INTEGER.matcher(value).matches()) {
            return Integer.parseInt(value);
        }
        if (DECIMAL.matcher(value).matches()) {
            return Double.parseDouble(value);
        }
        return value;
    }
}
