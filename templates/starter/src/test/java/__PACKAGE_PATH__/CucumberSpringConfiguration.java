package __PACKAGE__;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/** Connects Cucumber with Spring: page objects and step definitions are created per scenario. */
@CucumberContextConfiguration
@SpringBootTest(classes = SuiteApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CucumberSpringConfiguration {
}
