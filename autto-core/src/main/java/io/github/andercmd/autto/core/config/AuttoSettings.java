package io.github.andercmd.autto.core.config;

import io.github.andercmd.autto.core.security.Secrets;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Loads the framework configuration with Spring Boot's own machinery (relaxed binding, profiles, YAML,
 * {@code ${placeholders}}, {@link java.time.Duration} conversion) and exposes it to code that runs outside the
 * Spring context, such as the Cucumber report plugin. The Spring context publishes the very same instance as a bean.
 *
 * <p>Property sources, highest precedence first:
 *
 * <ol>
 *   <li>JVM system properties ({@code -Dautto.browser.name=firefox})
 *   <li>Environment variables ({@code AUTTO_BROWSER_NAME=firefox})
 *   <li>The local {@code .env} file (git-ignored)
 *   <li>{@code application-<profile>.yml} for each active profile (last profile wins)
 *   <li>{@code application.yml}
 *   <li>Defaults of {@link AuttoProperties}
 * </ol>
 *
 * <p>Profiles come from {@code spring.profiles.active} ({@code -Dspring.profiles.active=staging},
 * {@code SPRING_PROFILES_ACTIVE}, {@code .env}) or {@code spring.profiles.default}.
 */
public final class AuttoSettings {

    public static final String DOTENV_SOURCE = "dotenv";
    private static final Logger LOG = LoggerFactory.getLogger(AuttoSettings.class);
    private static final String[] EXTENSIONS = {"yml", "yaml", "properties"};
    private static final List<PropertySourceLoader> LOADERS =
            List.of(new YamlPropertySourceLoader(), new PropertiesPropertySourceLoader());

    private static volatile AuttoSettings instance;

    private final ConfigurableEnvironment environment;
    private final AuttoProperties properties;
    private final List<String> activeProfiles;
    private final Optional<Path> dotEnvFile;

    private AuttoSettings(ConfigurableEnvironment environment, List<String> activeProfiles, Optional<Path> dotEnvFile) {
        this.environment = environment;
        this.activeProfiles = List.copyOf(activeProfiles);
        this.dotEnvFile = dotEnvFile;
        try {
            this.properties = Binder.get(environment)
                    .bind("autto", Bindable.of(AuttoProperties.class))
                    .orElseGet(AuttoProperties::defaults);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid Autto configuration: " + rootMessage(e), e);
        }
    }

    /** Process-wide settings, loaded on first access. */
    public static AuttoSettings get() {
        AuttoSettings local = instance;
        if (local == null) {
            synchronized (AuttoSettings.class) {
                local = instance;
                if (local == null) {
                    local = load(System.getenv(), System.getProperties());
                    instance = local;
                    LOG.info("Autto configuration loaded · profiles {} · .env {}", local.activeProfiles,
                            local.dotEnvFile.map(Path::toString).orElse("not found"));
                }
            }
        }
        return local;
    }

    /** Forces the configuration to be read again on the next {@link #get()} call. Intended for tests. */
    public static synchronized void reset() {
        instance = null;
    }

    /** Loads the configuration from explicit sources (classpath files are always read). */
    public static AuttoSettings load(Map<String, String> environmentVariables, Properties systemProperties) {
        Map<String, String> system = toMap(systemProperties);
        Optional<Path> dotEnvFile = DotEnv.locate(environmentVariables, system);
        Map<String, String> dotEnv = dotEnvFile.map(DotEnv::read).orElse(Map.of());

        Secrets.registerAll(environmentVariables);
        Secrets.registerAll(dotEnv);
        Secrets.registerAll(system);

        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.addLast(new PropertiesPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                systemProperties));
        sources.addLast(new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Collections.unmodifiableMap(new LinkedHashMap<>(environmentVariables))));
        sources.addLast(dotEnvSource(dotEnv));

        List<PropertySource<?>> base = loadClasspath("application");
        base.forEach(sources::addLast);

        List<String> profiles = resolveProfiles(environment);
        environment.setActiveProfiles(profiles.toArray(String[]::new));
        // later profiles have higher precedence, and all of them override application.yml
        String firstBase = base.isEmpty() ? null : base.getFirst().getName();
        for (String profile : profiles) {
            for (PropertySource<?> source : loadClasspath("application-" + profile)) {
                if (firstBase != null) {
                    sources.addBefore(firstBase, source);
                } else {
                    sources.addLast(source);
                }
                firstBase = source.getName();
            }
        }
        return new AuttoSettings(environment, profiles, dotEnvFile);
    }

    /** A {@code .env} source that understands environment-variable names ({@code AUTTO_BROWSER_NAME}). */
    public static PropertySource<?> dotEnvSource(Map<String, String> values) {
        return new SystemEnvironmentPropertySource(DOTENV_SOURCE,
                Collections.unmodifiableMap(new LinkedHashMap<>(values)));
    }

    // ------------------------------------------------------------------ accessors

    public AuttoProperties properties() {
        return properties;
    }

    public List<String> activeProfiles() {
        return activeProfiles;
    }

    public Optional<Path> dotEnvFile() {
        return dotEnvFile;
    }

    public ConfigurableEnvironment environment() {
        return environment;
    }

    /** Any property, with placeholders resolved ({@code autto.base-url}, {@code SAUCE_PASSWORD}, ...). */
    public Optional<String> find(String key) {
        String value = environment.getProperty(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    /** Default HTTP headers of the API client: {@code autto.api.headers.*}. */
    public Map<String, String> apiHeaders() {
        return subtree("autto.api.headers.");
    }

    /** W3C / vendor capabilities: {@code autto.execution.capabilities.*}. */
    public Map<String, String> capabilities() {
        return subtree("autto.execution.capabilities.");
    }

    /** Browser preferences: {@code autto.browser.prefs.*}. */
    public Map<String, String> browserPrefs() {
        return subtree("autto.browser.prefs.");
    }

    /** Extra rows of the report dashboard: {@code autto.report.info.*}. */
    public Map<String, String> reportInfo() {
        return subtree("autto.report.info.");
    }

    /**
     * Every property under {@code prefix} (which must end with a dot) with its <b>original</b> name: characters
     * such as {@code :} are kept and Spring's {@code [escaping brackets]} are removed, while list indexes such as
     * {@code args[0]} are preserved. Environment-variable sources are skipped because they cannot express
     * those names.
     */
    public Map<String, String> subtree(String prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof SystemEnvironmentPropertySource
                    || !(source instanceof EnumerablePropertySource<?> e)) {
                continue;
            }
            String bracketPrefix = prefix.substring(0, prefix.length() - 1) + "[";
            for (String name : e.getPropertyNames()) {
                boolean dotted = name.startsWith(prefix) && name.length() > prefix.length();
                boolean bracketed = name.startsWith(bracketPrefix);
                if (dotted || bracketed) {
                    String key = (dotted ? name.substring(prefix.length()) : name.substring(bracketPrefix.length() - 1))
                            .replaceAll("\\[([^\\]0-9][^\\]]*)]", "$1");
                    String value = environment.getProperty(name);
                    if (value != null && !value.isBlank()) {
                        result.putIfAbsent(key, value.trim());
                    }
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> resolveProfiles(ConfigurableEnvironment environment) {
        String active = environment.getProperty("spring.profiles.active");
        if (active == null || active.isBlank()) {
            active = environment.getProperty("spring.profiles.default", "");
        }
        return Arrays.stream(active.split(",")).map(String::trim).filter(p -> !p.isEmpty()).distinct().toList();
    }

    private static List<PropertySource<?>> loadClasspath(String baseName) {
        List<PropertySource<?>> result = new ArrayList<>();
        for (String extension : EXTENSIONS) {
            Resource resource = new ClassPathResource(baseName + "." + extension, AuttoSettings.class.getClassLoader());
            if (!resource.exists()) {
                continue;
            }
            for (PropertySourceLoader loader : LOADERS) {
                if (Arrays.asList(loader.getFileExtensions()).contains(extension)) {
                    try {
                        result.addAll(loader.load("classpath:" + baseName + "." + extension, resource));
                    } catch (IOException e) {
                        throw new UncheckedIOException("Unable to read " + resource, e);
                    }
                }
            }
        }
        return result;
    }

    private static Map<String, String> toMap(Properties properties) {
        Map<String, String> map = new LinkedHashMap<>();
        properties.stringPropertyNames().forEach(name -> map.put(name, properties.getProperty(name)));
        return map;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        StringBuilder message = new StringBuilder(String.valueOf(error.getMessage()));
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
            message.append(" → ").append(current.getMessage());
        }
        return message.toString();
    }
}
