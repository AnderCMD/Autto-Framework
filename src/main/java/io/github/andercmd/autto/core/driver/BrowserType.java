package io.github.andercmd.autto.core.driver;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Browsers supported out of the box. */
public enum BrowserType {
    CHROME("chrome"),
    FIREFOX("firefox"),
    EDGE("MicrosoftEdge"),
    SAFARI("safari");

    private final String w3cName;

    BrowserType(String w3cName) {
        this.w3cName = w3cName;
    }

    /** Name used in the W3C {@code browserName} capability. */
    public String w3cName() {
        return w3cName;
    }

    public boolean isChromium() {
        return this == CHROME || this == EDGE;
    }

    public static BrowserType from(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "chrome", "googlechrome", "google-chrome", "chromium" -> CHROME;
            case "firefox", "ff", "gecko" -> FIREFOX;
            case "edge", "msedge", "microsoftedge" -> EDGE;
            case "safari", "webkit" -> SAFARI;
            default -> throw new IllegalArgumentException("Unsupported browser '" + value + "'. Use one of: "
                    + Arrays.stream(values()).map(b -> b.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", ")));
        };
    }
}
