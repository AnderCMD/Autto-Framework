package io.github.andercmd.autto.core.spring;

import io.cucumber.spring.ScenarioScope;
import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.context.ScenarioContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration of the Autto engine. Any test project that depends on {@code autto-core} and
 * starts a Spring context ({@code @CucumberContextConfiguration @SpringBootTest}) gets these beans.
 *
 * <p>{@link AuttoSettings} is the single source of truth: the same instance is used by the Cucumber report plugin
 * (which runs outside Spring) and by the beans below. Every bean can be overridden by declaring your own.
 */
@AutoConfiguration
public class AuttoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuttoSettings auttoSettings() {
        return AuttoSettings.get();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuttoProperties auttoProperties(AuttoSettings settings) {
        return settings.properties();
    }

    @Bean
    @ScenarioScope
    @ConditionalOnMissingBean
    public ScenarioContext scenarioContext() {
        return new ScenarioContext();
    }
}
