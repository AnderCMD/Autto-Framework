package io.github.andercmd.autto.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layered, immutable configuration for the whole framework.
 *
 * <p>Values are resolved from the following sources, where each layer overrides the previous one:
 *
 * <ol>
 *   <li>{@code autto.properties} on the classpath (project defaults)
 *   <li>{@code environments/<env>.properties} on the classpath (environment specific values)
 *   <li>Environment variables prefixed with {@code AUTTO_} ({@code browser.headless} &rarr; {@code AUTTO_BROWSER_HEADLESS})
 *   <li>JVM system properties ({@code -Dbrowser.headless=true})
 * </ol>
 *
 * <p>Blank values are treated as "not set" so a higher layer can never accidentally erase a value with an
 * empty string.
 *
 * <p>Values may reference other keys or environment variables with {@code ${NAME}} or {@code ${NAME:default}}, e.g.
 * {@code remote.url=https://${BROWSERSTACK_USERNAME}:${BROWSERSTACK_ACCESS_KEY}@hub.browserstack.com/wd/hub}.
 */
public final class AuttoConfig {

    public static final String DEFAULT_FILE = "autto.properties";
    public static final String ENVIRONMENTS_FOLDER = "environments/";
    public static final String ENV_VAR_PREFIX = "AUTTO_";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");

    private static volatile AuttoConfig instance;

    private final Map<String, String> values;
    private final Map<String, String> environment;

    AuttoConfig(Map<String, String> values, Map<String, String> environment) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.environment = Map.copyOf(environment);
    }

    /** Returns the process-wide configuration, loading it on first access. */
    public static AuttoConfig get() {
        AuttoConfig local = instance;
        if (local == null) {
            synchronized (AuttoConfig.class) {
                local = instance;
                if (local == null) {
                    local = load(System.getenv(), System.getProperties());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Forces the configuration to be read again on the next {@link #get()} call. Mainly useful in tests. */
    public static synchronized void reset() {
        instance = null;
    }

    static AuttoConfig load(Map<String, String> environment, Properties systemProperties) {
        Map<String, String> merged = new LinkedHashMap<>();
        Map<String, String> envOverrides = fromEnvironment(environment);
        Map<String, String> sysOverrides = fromProperties(systemProperties);

        merge(merged, readClasspath(DEFAULT_FILE));

        String env = firstNonBlank(sysOverrides.get(ConfigKeys.ENV), envOverrides.get(ConfigKeys.ENV), merged.get(ConfigKeys.ENV))
                .orElse("qa");
        merge(merged, readClasspath(ENVIRONMENTS_FOLDER + env + ".properties"));
        merge(merged, envOverrides);
        merge(merged, sysOverrides);
        merged.put(ConfigKeys.ENV, env);
        return new AuttoConfig(merged, environment);
    }

    /** Creates a configuration from explicit values only. Handy for unit tests and programmatic usage. */
    public static AuttoConfig of(Map<String, String> values) {
        Map<String, String> clean = new LinkedHashMap<>();
        merge(clean, values);
        return new AuttoConfig(clean, Map.of());
    }

    // ------------------------------------------------------------------ typed accessors

    /** Returns the value of {@code key} with {@code ${...}} placeholders resolved, or empty when not set. */
    public Optional<String> find(String key) {
        String value = values.get(key);
        return value == null || value.isBlank() ? Optional.empty()
                : Optional.of(resolvePlaceholders(key, value.trim(), values, environment, 0));
    }

    public String get(String key, String defaultValue) {
        return find(key).orElse(defaultValue);
    }

    public String require(String key) {
        return find(key).orElseThrow(() -> new IllegalStateException(
                "Missing required configuration '" + key + "'. Set it in autto.properties, in the environment file, "
                        + "as -D" + key + "=... or as environment variable " + toEnvVar(key)));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return find(key).map(v -> switch (v.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "y", "1", "on" -> true;
            case "false", "no", "n", "0", "off" -> false;
            default -> throw invalid(key, v, "a boolean");
        }).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return find(key).map(v -> parse(key, v, Integer::parseInt, "an integer")).orElse(defaultValue);
    }

    public Duration getSeconds(String key, long defaultSeconds) {
        return find(key)
                .map(v -> Duration.ofMillis(Math.round(parse(key, v, Double::parseDouble, "a number of seconds") * 1000)))
                .orElse(Duration.ofSeconds(defaultSeconds));
    }

    public List<String> getList(String key) {
        return find(key)
                .map(v -> Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList())
                .orElse(List.of());
    }

    public <E extends Enum<E>> E getEnum(String key, Class<E> type, E defaultValue) {
        return find(key).map(v -> {
            String normalized = v.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            try {
                return Enum.valueOf(type, normalized);
            } catch (IllegalArgumentException e) {
                throw invalid(key, v, "one of " + Arrays.toString(type.getEnumConstants()).toLowerCase(Locale.ROOT));
            }
        }).orElse(defaultValue);
    }

    /** Returns every key starting with {@code prefix}, with the prefix removed. */
    public Map<String, String> withPrefix(String prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((k, v) -> {
            if (k.startsWith(prefix) && k.length() > prefix.length() && v != null && !v.isBlank()) {
                result.put(k.substring(prefix.length()), resolvePlaceholders(k, v.trim(), values, environment, 0));
            }
        });
        return result;
    }

    /** Raw values, placeholders not resolved. */
    public Map<String, String> asMap() {
        return values;
    }

    // ------------------------------------------------------------------ helpers

    public static String toEnvVar(String key) {
        return ENV_VAR_PREFIX + key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    private static Map<String, String> fromEnvironment(Map<String, String> environment) {
        Map<String, String> result = new LinkedHashMap<>();
        environment.forEach((k, v) -> {
            if (k.startsWith(ENV_VAR_PREFIX) && k.length() > ENV_VAR_PREFIX.length()) {
                String key = k.substring(ENV_VAR_PREFIX.length()).toLowerCase(Locale.ROOT).replace('_', '.');
                result.put(key, v);
            }
        });
        return result;
    }

    private static Map<String, String> fromProperties(Properties properties) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            result.put(name, properties.getProperty(name));
        }
        return result;
    }

    private static Map<String, String> readClasspath(String resource) {
        ClassLoader loader = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
                .orElse(AuttoConfig.class.getClassLoader());
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                return Map.of();
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return fromProperties(properties);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read configuration file " + resource, e);
        }
    }

    private static void merge(Map<String, String> target, Map<String, String> source) {
        source.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                target.put(k, v.trim());
            }
        });
    }

    static String resolvePlaceholders(String key, String value, Map<String, String> config, Map<String, String> environment,
            int depth) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        if (depth > 5) {
            throw new IllegalStateException("Configuration '" + key + "' has circular placeholders: " + value);
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String fallback = matcher.group(2);
            String replacement = Optional.ofNullable(config.get(name))
                    .or(() -> Optional.ofNullable(environment.get(name)))
                    .filter(v -> !v.isBlank())
                    .map(v -> resolvePlaceholders(name, v, config, environment, depth + 1))
                    .orElse(fallback);
            if (replacement == null) {
                throw new IllegalStateException("Configuration '" + key + "' references ${" + name
                        + "} but no configuration key or environment variable with that name exists");
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    @SafeVarargs
    private static <T> Optional<T> firstNonBlank(T... candidates) {
        for (T candidate : candidates) {
            if (candidate != null && !candidate.toString().isBlank()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static <T> T parse(String key, String value, Function<String, T> parser, String expected) {
        try {
            return parser.apply(value.trim());
        } catch (RuntimeException e) {
            throw invalid(key, value, expected);
        }
    }

    private static IllegalArgumentException invalid(String key, String value, String expected) {
        return new IllegalArgumentException("Configuration '" + key + "' = '" + value + "' is not " + expected);
    }
}
