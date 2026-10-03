package io.github.andercmd.autto.core.driver;

import java.util.Locale;
import java.util.Optional;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;

/** Human friendly description of a running session (used for the "Device" view of the report). */
public record SessionDetails(String browserName, String browserVersion, String platform) {

    public static SessionDetails of(WebDriver driver) {
        if (!(driver instanceof HasCapabilities withCapabilities)) {
            return new SessionDetails("unknown", "", "");
        }
        Capabilities caps = withCapabilities.getCapabilities();
        String name = Optional.ofNullable(caps.getBrowserName()).filter(s -> !s.isBlank())
                .or(() -> Optional.ofNullable(caps.getCapability("appium:deviceName")).map(Object::toString))
                .orElse("unknown");
        String version = Optional.ofNullable(caps.getBrowserVersion())
                .or(() -> Optional.ofNullable(caps.getCapability("appium:platformVersion")).map(Object::toString))
                .orElse("");
        String platform = caps.getPlatformName() == null ? "" : caps.getPlatformName().toString();
        return new SessionDetails(name, version, platform);
    }

    /** For example {@code chrome-141.0-linux}. Extent uses device names as identifiers, so no spaces are used. */
    public String label() {
        StringBuilder label = new StringBuilder(browserName);
        if (!browserVersion.isBlank()) {
            label.append('-').append(majorMinor(browserVersion));
        }
        if (!platform.isBlank()) {
            label.append('-').append(platform);
        }
        return label.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private static String majorMinor(String version) {
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
    }
}
