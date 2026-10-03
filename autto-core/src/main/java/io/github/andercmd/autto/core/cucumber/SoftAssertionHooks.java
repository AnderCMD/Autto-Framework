package io.github.andercmd.autto.core.cucumber;

import io.cucumber.java.After;
import org.assertj.core.api.SoftAssertions;

/**
 * Verifies the scenario's {@link SoftAssertions} once all steps have run, so every soft failure of a scenario is
 * reported together. Runs before {@link BrowserHooks} collects the failure evidence (higher order = earlier).
 *
 * <pre>{@code
 * public CartSteps(SoftAssertions softly) { this.softly = softly; }
 *
 * softly.assertThat(cart.count()).isEqualTo(2);
 * softly.assertThat(cart.total()).isEqualTo("$39.98");   // both are checked, failures are listed together
 * }</pre>
 */
public class SoftAssertionHooks {

    private final SoftAssertions softly;

    public SoftAssertionHooks(SoftAssertions softly) {
        this.softly = softly;
    }

    @After(order = 100)
    public void verifySoftAssertions() {
        softly.assertAll();
    }
}
