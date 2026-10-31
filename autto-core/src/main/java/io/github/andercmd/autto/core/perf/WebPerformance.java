package io.github.andercmd.autto.core.perf;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.report.Report;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.JavascriptExecutor;

/**
 * Measures the page the browser is currently showing (Navigation Timing and Paint Timing APIs) and checks it against
 * the budgets of {@code autto.performance.*}.
 *
 * <pre>{@code
 * WebPerformance.Metrics metrics = WebPerformance.measure();
 * metrics.assertWithinBudget();      // fails listing every metric over its budget
 * }</pre>
 *
 * <p>LCP and CLS are only reported by Chromium browsers; budgets of metrics the browser does not provide are
 * skipped.
 */
public final class WebPerformance {

    private static final String SCRIPT = """
            const nav = performance.getEntriesByType('navigation')[0] || {};
            const fcp = performance.getEntriesByName('first-contentful-paint')[0];
            const done = arguments[arguments.length - 1];
            let lcp = null, cls = 0;
            try {
              new PerformanceObserver(l => { const e = l.getEntries(); lcp = e[e.length - 1].startTime; })
                  .observe({type: 'largest-contentful-paint', buffered: true});
              new PerformanceObserver(l => { l.getEntries().forEach(e => { if (!e.hadRecentInput) cls += e.value; }); })
                  .observe({type: 'layout-shift', buffered: true});
            } catch (e) { lcp = null; cls = null; }
            setTimeout(() => done({
              ttfb: nav.responseStart || null,
              domContentLoaded: nav.domContentLoadedEventEnd || null,
              load: nav.loadEventEnd || null,
              fcp: fcp ? fcp.startTime : null,
              lcp: lcp,
              cls: cls,
              transferSize: nav.transferSize || null
            }), 100);
            """;

    private WebPerformance() {
    }

    /** Measures the current page and adds the numbers to the report. */
    public static Metrics measure() {
        Object raw = ((JavascriptExecutor) DriverManager.driver()).executeAsyncScript(SCRIPT);
        Metrics metrics = Metrics.fromBrowser(asMap(raw));
        Report.table("Web performance", metrics.asRows());
        return metrics;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object raw) {
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /**
     * Measured values; {@code null} when the browser does not provide a metric.
     *
     * @param ttfb time to first byte (ms)
     * @param fcp first contentful paint (ms)
     * @param lcp largest contentful paint (ms)
     * @param cls cumulative layout shift
     * @param load load event end (ms)
     * @param domContentLoaded DOMContentLoaded end (ms)
     * @param transferSize bytes transferred by the document
     */
    public record Metrics(Double ttfb, Double fcp, Double lcp, Double cls, Double load, Double domContentLoaded,
            Double transferSize) {

        static Metrics fromBrowser(Map<String, Object> values) {
            return new Metrics(number(values, "ttfb"), number(values, "fcp"), number(values, "lcp"),
                    number(values, "cls"), number(values, "load"), number(values, "domContentLoaded"),
                    number(values, "transferSize"));
        }

        /** Fails with one message listing every metric above the configured budget. */
        public void assertWithinBudget() {
            assertWithinBudget(AuttoSettings.get().properties().performance());
        }

        public void assertWithinBudget(AuttoProperties.Performance budget) {
            List<String> violations = violations(budget);
            if (!violations.isEmpty()) {
                throw new AssertionError("Performance budget exceeded:\n - " + String.join("\n - ", violations));
            }
        }

        List<String> violations(AuttoProperties.Performance budget) {
            List<String> violations = new ArrayList<>();
            check(violations, "TTFB", ttfb, budget.ttfb());
            check(violations, "FCP", fcp, budget.fcp());
            check(violations, "LCP", lcp, budget.lcp());
            check(violations, "load", load, budget.load());
            if (cls != null && budget.cls() > 0 && cls > budget.cls()) {
                violations.add("CLS %.3f > %.3f".formatted(cls, budget.cls()));
            }
            return violations;
        }

        Map<String, Object> asRows() {
            Map<String, Object> rows = new LinkedHashMap<>();
            rows.put("TTFB (ms)", format(ttfb));
            rows.put("FCP (ms)", format(fcp));
            rows.put("LCP (ms)", format(lcp));
            rows.put("CLS", cls == null ? "n/a" : "%.3f".formatted(cls));
            rows.put("DOMContentLoaded (ms)", format(domContentLoaded));
            rows.put("Load (ms)", format(load));
            rows.put("Document transfer (bytes)", format(transferSize));
            return rows;
        }

        private static void check(List<String> violations, String name, Double actual, Duration budget) {
            if (actual != null && !budget.isZero() && !budget.isNegative() && actual > budget.toMillis()) {
                violations.add("%s %.0f ms > %d ms".formatted(name, actual, budget.toMillis()));
            }
        }

        private static String format(Double value) {
            return value == null ? "n/a" : "%.0f".formatted(value);
        }

        private static Double number(Map<String, Object> values, String key) {
            return values.get(key) instanceof Number n ? n.doubleValue() : null;
        }
    }
}
