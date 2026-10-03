package io.github.andercmd.autto.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Connects Cucumber with Spring: one application context for the whole run, while page objects, step definitions
 * and {@code ScenarioContext} are created per scenario ({@code @ScenarioScope}).
 */
@CucumberContextConfiguration
@SpringBootTest(classes = E2eTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CucumberSpringConfiguration {
}
