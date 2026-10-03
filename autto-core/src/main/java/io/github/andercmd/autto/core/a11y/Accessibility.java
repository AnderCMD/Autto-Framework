package io.github.andercmd.autto.core.a11y;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.report.Report;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.WebDriver;

/**
 * Accessibility audits of the current page with <a href="https://github.com/dequelabs/axe-core">axe-core</a>.
 *
 * <pre>{@code
 * AccessibilityResult result = Accessibility.scan();          // whole page
 * AccessibilityResult form = Accessibility.scan("#login");     // only some regions (CSS selectors)
 * result.assertNoViolations();                                 // fails on autto.accessibility.fail-on or worse
 * }</pre>
 *
 * <p>The rules come from {@code autto.accessibility.tags} (WCAG 2.1 A/AA by default) minus
 * {@code autto.accessibility.disabled-rules}. Every scan adds a summary table to the report.
 */
public final class Accessibility {

    private Accessibility() {
    }

    /** Audits the page of the current browser (optionally only the regions matching the CSS selectors). */
    public static AccessibilityResult scan(String... include) {
        return scan(DriverManager.driver(), AuttoSettings.get().properties().accessibility(), include);
    }

    public static AccessibilityResult scan(WebDriver driver, AuttoProperties.Accessibility config,
            String... include) {
        AxeBuilder axe = new AxeBuilder().withTags(config.tags());
        if (!config.disabledRules().isEmpty()) {
            axe.disableRules(config.disabledRules());
        }
        if (include.length > 0) {
            axe.include(List.of(include));
        }
        Results results = axe.analyze(driver);
        if (results.isErrored()) {
            throw new IllegalStateException("Accessibility scan failed: " + results.getErrorMessage());
        }
        List<AccessibilityResult.Violation> violations = new ArrayList<>();
        for (Rule rule : results.getViolations()) {
            List<String> targets = rule.getNodes().stream().map(n -> String.valueOf(n.getTarget())).toList();
            violations.add(new AccessibilityResult.Violation(rule.getId(), impact(rule.getImpact()), rule.getHelp(),
                    rule.getHelpUrl(), targets));
        }
        AccessibilityResult result = new AccessibilityResult(results.getUrl(), violations, config.failOn());
        report(result);
        return result;
    }

    static AuttoProperties.Impact impact(String value) {
        if (value == null || value.isBlank()) {
            return AuttoProperties.Impact.MINOR;
        }
        return AuttoProperties.Impact.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static void report(AccessibilityResult result) {
        if (result.violations().isEmpty()) {
            Report.pass("Accessibility: no violations on " + result.url());
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Rule", "Impact", "Elements", "Help"));
        result.violations().forEach(v -> rows.add(List.of(v.rule(), v.impact().name().toLowerCase(Locale.ROOT),
                String.valueOf(v.targets().size()), v.help() + " · " + v.helpUrl())));
        Report.warning("Accessibility: " + result.violations().size() + " violated rule(s) on " + result.url());
        Report.table("Accessibility violations", rows);
    }
}
