package io.github.andercmd.autto.core.driver;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.BrowserType;
import io.github.andercmd.autto.core.config.DriverResolution;
import org.junit.jupiter.api.Test;

class DriverResolverTest {

    private static AuttoProperties props(DriverResolution resolution, String version) {
        AuttoProperties defaults = AuttoProperties.defaults();
        AuttoProperties.Browser b = defaults.browser();
        return new AuttoProperties(null,
                new AuttoProperties.Browser(b.name(), version, null, null, null, null, null, null, null, null, null,
                        null, null),
                new AuttoProperties.Driver(resolution, null, null, null, null, null),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void webDriverManagerIsTheDefault() {
        assertThat(DriverResolver.strategyFor(BrowserType.CHROME, props(null, null)))
                .isEqualTo(DriverResolver.Strategy.WEBDRIVERMANAGER);
        assertThat(DriverResolver.strategyFor(BrowserType.FIREFOX, props(DriverResolution.WEBDRIVERMANAGER, null)))
                .isEqualTo(DriverResolver.Strategy.WEBDRIVERMANAGER);
    }

    @Test
    void seleniumManagerWhenConfiguredOrWhenABrowserVersionIsRequested() {
        assertThat(DriverResolver.strategyFor(BrowserType.EDGE, props(DriverResolution.SELENIUM_MANAGER, null)))
                .isEqualTo(DriverResolver.Strategy.SELENIUM_MANAGER);
        assertThat(DriverResolver.strategyFor(BrowserType.CHROME, props(null, "beta")))
                .as("Selenium Manager can download the requested browser")
                .isEqualTo(DriverResolver.Strategy.SELENIUM_MANAGER);
    }

    @Test
    void safariNeedsNoDriverDownload() {
        assertThat(DriverResolver.strategyFor(BrowserType.SAFARI, props(null, null)))
                .isEqualTo(DriverResolver.Strategy.NONE);
    }
}
