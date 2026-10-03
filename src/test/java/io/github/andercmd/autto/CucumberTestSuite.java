package io.github.andercmd.autto;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Entry point of the Cucumber scenarios ({@code mvn test}).
 *
 * <p>All options live in {@code src/test/resources/junit-platform.properties} and can be overridden with system
 * properties, for example:
 *
 * <pre>
 * mvn test -Dcucumber.filter.tags="@smoke and not @wip"
 * mvn test -Dcucumber.features=classpath:features/login
 * mvn test -Dbrowser=firefox -Dbrowser.headless=true
 * </pre>
 */
@Suite
@SuiteDisplayName("Autto · Cucumber scenarios")
@IncludeEngines("cucumber")
@SelectPackages("features")
public class CucumberTestSuite {
}
