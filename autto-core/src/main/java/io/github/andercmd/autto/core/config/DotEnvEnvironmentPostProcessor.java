package io.github.andercmd.autto.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Adds the local {@code .env} file to the Spring context, right after the real environment variables, so
 * {@code application.yml} can reference its entries ({@code password: ${SAUCE_PASSWORD}}) and it can even select
 * profiles ({@code SPRING_PROFILES_ACTIVE=staging}). Runs before Spring Boot processes config data.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(AuttoSettings.DOTENV_SOURCE)) {
            return;
        }
        Map<String, String> system = new LinkedHashMap<>();
        System.getProperties().stringPropertyNames().forEach(n -> system.put(n, System.getProperty(n)));
        DotEnv.locate(System.getenv(), system).map(DotEnv::read).ifPresent(values -> {
            String systemEnvironment = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
            if (environment.getPropertySources().contains(systemEnvironment)) {
                environment.getPropertySources().addAfter(systemEnvironment, AuttoSettings.dotEnvSource(values));
            } else {
                environment.getPropertySources().addLast(AuttoSettings.dotEnvSource(values));
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
