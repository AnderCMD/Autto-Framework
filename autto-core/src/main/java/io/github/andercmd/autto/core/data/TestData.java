package io.github.andercmd.autto.core.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.andercmd.autto.core.config.AuttoSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.datafaker.Faker;

/**
 * Loads JSON or YAML ({@code .yml}/{@code .yaml}) test data from {@code src/test/resources/testdata} and creates
 * random data.
 *
 * <p>String values may contain {@code ${NAME}} or {@code ${NAME:default}} placeholders, resolved against the
 * framework configuration: {@code .env}, environment variables, system properties and {@code application.yml}.
 * This keeps secrets out of the repository:
 *
 * <pre>{"password": "${ADMIN_PASSWORD}"}</pre>
 */
public final class TestData {

    private static final String ROOT = "testdata/";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .findAndAddModules()
            .build();
    private static final ObjectMapper YAML = YAMLMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .findAndAddModules()
            .build();
    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(() -> new Faker(Locale.ENGLISH));

    private TestData() {
    }

    /** Reads {@code testdata/<path>} and maps it to {@code type}. */
    public static <T> T load(String path, Class<T> type) {
        return MAPPER.convertValue(tree(path), type);
    }

    /**
     * Reads {@code testdata/<path>} and maps it to a generic type, e.g.
     * {@code new TypeReference<Map<String, User>>() {}}.
     */
    public static <T> T load(String path, TypeReference<T> type) {
        return MAPPER.convertValue(tree(path), type);
    }

    /** Reads one entry of an object file: {@code TestData.entry("login/users.json", "standard", User.class)}. */
    public static <T> T entry(String path, String key, Class<T> type) {
        JsonNode node = tree(path).get(key);
        if (node == null) {
            throw new IllegalArgumentException("Test data '" + key + "' not found in " + ROOT + path);
        }
        return MAPPER.convertValue(node, type);
    }

    /** Random data generator (one instance per thread). */
    public static Faker faker() {
        return FAKER.get();
    }

    static JsonNode tree(String path) {
        String resource = ROOT + path;
        try (InputStream in = TestData.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Test data file not found on the classpath: " + resource);
            }
            ObjectMapper reader = path.endsWith(".yml") || path.endsWith(".yaml") ? YAML : MAPPER;
            return resolve(reader.readTree(in));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read test data " + resource, e);
        }
    }

    private static JsonNode resolve(JsonNode node) {
        if (node.isTextual()) {
            return new TextNode(resolve(node.asText()));
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            for (Map.Entry<String, JsonNode> field : object.properties()) {
                field.setValue(resolve(field.getValue()));
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                ((ArrayNode) node).set(i, resolve(node.get(i)));
            }
        }
        return node;
    }

    static String resolve(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String fallback = matcher.group(2);
            String replacement = AuttoSettings.get().find(key).orElseGet(() -> {
                if (fallback == null) {
                    throw new IllegalStateException("Test data placeholder ${" + key + "} has no value. Define it in "
                            + "the .env file, as environment variable or in application.yml");
                }
                return fallback;
            });
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
