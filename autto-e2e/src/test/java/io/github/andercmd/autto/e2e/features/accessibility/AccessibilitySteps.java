package io.github.andercmd.autto.e2e.features.accessibility;

import io.cucumber.java.en.Then;
import io.github.andercmd.autto.core.a11y.Accessibility;
import io.github.andercmd.autto.core.a11y.AccessibilityResult;
import io.github.andercmd.autto.core.config.AuttoProperties.Impact;
import java.util.Locale;

/** Reusable accessibility steps: every audit is summarized in the report. */
public class AccessibilitySteps {

    @Then("the page has no accessibility violations")
    public void thePageHasNoAccessibilityViolations() {
        Accessibility.scan().assertNoViolations();
    }

    @Then("the page has no {word} accessibility violations")
    public void thePageHasNoAccessibilityViolationsOfImpact(String impact) {
        AccessibilityResult result = Accessibility.scan();
        result.assertNoViolations(Impact.valueOf(impact.toUpperCase(Locale.ROOT)));
    }
}
