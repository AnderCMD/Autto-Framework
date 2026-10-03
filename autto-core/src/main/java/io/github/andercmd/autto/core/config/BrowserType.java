package io.github.andercmd.autto.core.config;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import java.util.Optional;

/** Browsers supported out of the box. */
public enum BrowserType {
    CHROME(DriverManagerType.CHROME),
    CHROMIUM(DriverManagerType.CHROMIUM),
    FIREFOX(DriverManagerType.FIREFOX),
    EDGE(DriverManagerType.EDGE),
    SAFARI(DriverManagerType.SAFARI);

    private final DriverManagerType driverManagerType;

    BrowserType(DriverManagerType driverManagerType) {
        this.driverManagerType = driverManagerType;
    }

    /** WebDriverManager type, empty for Safari (safaridriver ships with macOS). */
    public Optional<DriverManagerType> driverManagerType() {
        return this == SAFARI ? Optional.empty() : Optional.of(driverManagerType);
    }

    public boolean isChromium() {
        return this == CHROME || this == CHROMIUM || this == EDGE;
    }
}
