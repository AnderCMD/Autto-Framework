package io.github.andercmd.autto.core.ui;

import io.cucumber.spring.ScenarioScope;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * Marks a page object (or page component) as a Spring bean with one instance per Cucumber scenario.
 *
 * <pre>{@code
 * @PageObject
 * public class LoginPage extends BasePage { ... }
 *
 * public class LoginSteps {
 *     public LoginSteps(LoginPage loginPage) { ... }   // injected
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@ScenarioScope
public @interface PageObject {
}
