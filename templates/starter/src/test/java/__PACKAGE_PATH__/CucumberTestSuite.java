package __PACKAGE__;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Entry point of the scenarios: ./mvnw test -Dcucumber.filter.tags="@smoke" */
@Suite
@SuiteDisplayName("__ARTIFACT__ · Cucumber scenarios")
@IncludeEngines("cucumber")
@SelectPackages("features")
public class CucumberTestSuite {
}
