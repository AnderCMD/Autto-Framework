package io.github.andercmd.autto.core.a11y;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andercmd.autto.core.config.AuttoProperties.Impact;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccessibilityResultTest {

    private final AccessibilityResult result = new AccessibilityResult("https://shop.test", List.of(
            new AccessibilityResult.Violation("color-contrast", Impact.SERIOUS, "Low contrast", "https://help/1",
                    List.of(".price")),
            new AccessibilityResult.Violation("region", Impact.MODERATE, "Content outside landmarks",
                    "https://help/2", List.of("#footer", "#ad"))),
            Impact.SERIOUS);

    @Test
    void filtersByMinimumImpact() {
        assertThat(result.atLeast(Impact.CRITICAL)).isEmpty();
        assertThat(result.atLeast(Impact.SERIOUS)).extracting(AccessibilityResult.Violation::rule)
                .containsExactly("color-contrast");
        assertThat(result.atLeast(Impact.MINOR)).hasSize(2);
    }

    @Test
    void assertionUsesTheConfiguredThreshold() {
        assertThatThrownBy(result::assertNoViolations)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("color-contrast")
                .hasMessageNotContaining("region");
        assertThatCode(() -> result.assertNoViolations(Impact.CRITICAL)).doesNotThrowAnyException();
    }

    @Test
    void parsesAxeImpacts() {
        assertThat(Accessibility.impact("critical")).isEqualTo(Impact.CRITICAL);
        assertThat(Accessibility.impact(null)).isEqualTo(Impact.MINOR);
    }
}
