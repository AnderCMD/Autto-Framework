package io.github.andercmd.autto.core.driver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts flat {@code autto.execution.capabilities.*} entries into the nested structure expected by WebDriver.
 *
 * <pre>
 * se:recordVideo=true                -&gt; {"se:recordVideo": true}
 * bstack:options.os=Windows          -&gt; {"bstack:options": {"os": "Windows"}}
 * bstack:options.osVersion="11"      -&gt; {"bstack:options": {"os": "Windows", "osVersion": "11"}}
 * goog:chromeOptions.args[0]=--lang  -&gt; {"goog:chromeOptions": {"args": ["--lang"]}}     (YAML lists)
 * goog:chromeOptions.args=[a,b]      -&gt; {"goog:chromeOptions": {"args": ["a", "b"]}}
 * </pre>
 *
 * <p>Values are converted to booleans, integers or decimals when they look like one. Wrap a value in quotes to keep
 * it as a string.
 */
public final class CapabilitiesParser {

    private static final Pattern INTEGER = Pattern.compile("-?\\d{1,9}");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+\\.\\d+");
    private static final Pattern INDEXED = Pattern.compile("(.+)\\[(\\d+)]$");

    private CapabilitiesParser() {
    }

    public static Map<String, Object> parse(Map<String, String> flat) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, TreeMap<Integer, Object>> lists = new LinkedHashMap<>();
        flat.forEach((key, value) -> {
            Matcher indexed = INDEXED.matcher(key);
            if (indexed.matches()) {
                lists.computeIfAbsent(indexed.group(1), k -> new TreeMap<>())
                        .put(Integer.parseInt(indexed.group(2)), convert(value));
            } else {
                put(root, key.split("\\."), convert(value));
            }
        });
        lists.forEach((key, items) -> put(root, key.split("\\."), new ArrayList<>(items.values())));
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

    /** Converts a textual value to Boolean, Integer, Double, List or String. */
    public static Object convert(String raw) {
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
