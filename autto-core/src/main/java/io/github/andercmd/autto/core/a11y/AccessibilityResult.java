package io.github.andercmd.autto.core.a11y;

import io.github.andercmd.autto.core.config.AuttoProperties.Impact;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Outcome of an {@link Accessibility#scan(String...)}.
 *
 * @param url audited page
 * @param violations violated axe rules
 * @param failOn minimum impact rejected by {@link #assertNoViolations()}
 */
public record AccessibilityResult(String url, List<Violation> violations, Impact failOn) {

    public AccessibilityResult {
        violations = List.copyOf(violations);
    }

    /**
     * @param rule axe rule id (color-contrast, label, image-alt...)
     * @param impact severity
     * @param help short description of the problem
     * @param helpUrl how to fix it
     * @param targets CSS selectors of the offending elements
     */
    public record Violation(String rule, Impact impact, String help, String helpUrl, List<String> targets) {

        public Violation {
            targets = List.copyOf(targets);
        }
    }

    /** Violations with the given impact or worse. */
    public List<Violation> atLeast(Impact minimum) {
        return violations.stream().filter(v -> v.impact().compareTo(minimum) >= 0).toList();
    }

    /** Fails when there are violations of {@code autto.accessibility.fail-on} impact or worse. */
    public void assertNoViolations() {
        assertNoViolations(failOn);
    }

    /** Fails when there are violations of {@code minimum} impact or worse. */
    public void assertNoViolations(Impact minimum) {
        List<Violation> blocking = atLeast(minimum);
        if (!blocking.isEmpty()) {
            throw new AssertionError(blocking.size() + " accessibility violation(s) of impact " + minimum
                    + " or worse on " + url + ":\n" + blocking.stream()
                    .map(v -> "  - [" + v.impact() + "] " + v.rule() + ": " + v.help() + " " + v.targets())
                    .collect(Collectors.joining("\n")));
        }
    }
}
