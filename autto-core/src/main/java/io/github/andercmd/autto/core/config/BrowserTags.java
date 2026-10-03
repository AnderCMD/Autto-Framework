package io.github.andercmd.autto.core.config;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing of the browser-related Gherkin tags. */
public final class BrowserTags {

    private static final Pattern VIEWPORT = Pattern.compile("^@viewport:(\\d{2,5})x(\\d{2,5})$");

    private BrowserTags() {
    }

    /** Window size requested by a {@code @viewport:WIDTHxHEIGHT} tag. */
    public static Optional<Viewport> viewport(String tag) {
        Matcher matcher = VIEWPORT.matcher(tag);
        return matcher.matches()
                ? Optional.of(new Viewport(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))))
                : Optional.empty();
    }

    /** A window size in pixels. */
    public record Viewport(int width, int height) {
    }
}
