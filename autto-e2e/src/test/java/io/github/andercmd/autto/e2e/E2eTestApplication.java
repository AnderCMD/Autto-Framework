package io.github.andercmd.autto.e2e;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring configuration of the test suite: scans this package for {@code @PageObject}s and components and enables
 * the Autto auto-configuration. Add your own beans here (API clients, database helpers, test data builders...).
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
public class E2eTestApplication {
}
