package io.github.andercmd.autto.core.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andercmd.autto.core.config.AuttoProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebPerformanceTest {

    private static final AuttoProperties.Performance BUDGET =
            new AuttoProperties.Performance(null, null, null, null, null);

    @Test
    void metricsWithinBudgetPass() {
        WebPerformance.Metrics metrics = WebPerformance.Metrics.fromBrowser(
                Map.of("ttfb", 120, "fcp", 900.4, "lcp", 1500, "cls", 0.02, "load", 2000));

        metrics.assertWithinBudget(BUDGET);
        assertThat(metrics.asRows()).containsEntry("FCP (ms)", "900").containsEntry("CLS", "0.020");
    }

    @Test
    void everyViolationIsListed() {
        WebPerformance.Metrics metrics = WebPerformance.Metrics.fromBrowser(
                Map.of("ttfb", 2000, "fcp", 900, "lcp", 4000, "cls", 0.5, "load", 9000));

        assertThatThrownBy(() -> metrics.assertWithinBudget(BUDGET)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("TTFB 2000 ms > 800 ms", "LCP 4000 ms > 2500 ms", "CLS 0.500 > 0.100",
                        "load 9000 ms > 5000 ms")
                .hasMessageNotContaining("FCP");
    }

    @Test
    void metricsTheBrowserDoesNotProvideAreSkipped() {
        WebPerformance.Metrics metrics = WebPerformance.Metrics.fromBrowser(Map.of("ttfb", 100));

        metrics.assertWithinBudget(BUDGET);
        assertThat(metrics.asRows()).containsEntry("LCP (ms)", "n/a");
    }
}
