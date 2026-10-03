package __PACKAGE__;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Spring configuration of the suite: scans this package for page objects and enables the Autto engine. */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
public class SuiteApplication {
}
