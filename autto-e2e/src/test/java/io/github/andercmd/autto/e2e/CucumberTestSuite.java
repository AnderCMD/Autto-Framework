package io.github.andercmd.autto.e2e;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Entry point of the Cucumber scenarios ({@code mvn test}).
 *
 * <p>Cucumber options live in {@code junit-platform.properties}, framework options in {@code application.yml}.
 * Both can be overridden with system properties, for example:
 *
 * <pre>
 * ./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@smoke and not @wip"
 * ./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/login
 * ./mvnw -pl autto-e2e test -Dautto.browser.name=firefox -Dautto.browser.headless=true
 * ./mvnw -pl autto-e2e test -Dspring.profiles.active=staging,ci
 * </pre>
 */
@Suite
@SuiteDisplayName("Autto · Cucumber scenarios")
@IncludeEngines("cucumber")
@SelectPackages("features")
public class CucumberTestSuite {
}
