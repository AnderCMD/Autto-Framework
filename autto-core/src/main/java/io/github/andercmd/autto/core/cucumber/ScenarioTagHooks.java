package io.github.andercmd.autto.core.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.github.andercmd.autto.core.config.BrowserTags;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.report.Report;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;
import org.openqa.selenium.chromium.HasNetworkConditions;
import org.openqa.selenium.remote.Augmenter;

/**
 * Environment tags that change how a scenario's browser behaves, so common conditions need no code:
 *
 * <ul>
 *   <li>{@code @viewport:390x844} resizes the window (mobile and tablet layouts)
 *   <li>{@code @slow-network} throttles the network (Chromium): 400 kbps, 400 ms latency
 *   <li>{@code @offline} cuts the network (Chromium)
 * </ul>
 */
public class ScenarioTagHooks {

    @Before(value = "not @nobrowser", order = 20)
    public void applyTags(Scenario scenario) {
        for (String tag : scenario.getSourceTagNames()) {
            BrowserTags.viewport(tag).ifPresent(size -> {
                DriverManager.driver().manage().window().setSize(new Dimension(size.width(), size.height()));
                Report.info("Viewport " + size.width() + "x" + size.height());
            });
            switch (tag) {
                case "@slow-network" -> conditions(false, 400, 400 * 1024 / 8);
                case "@offline" -> conditions(true, 0, 0);
                default -> {
                }
            }
        }
    }

    private static void conditions(boolean offline, int latencyMs, int throughputBytes) {
        Object augmented = new Augmenter().augment(DriverManager.driver());
        if (!(augmented instanceof HasNetworkConditions conditions)) {
            Report.warning("Network conditions are only supported by Chromium browsers");
            return;
        }
        ChromiumNetworkConditions network = new ChromiumNetworkConditions();
        network.setOffline(offline);
        network.setLatency(java.time.Duration.ofMillis(latencyMs));
        network.setDownloadThroughput(throughputBytes);
        network.setUploadThroughput(throughputBytes);
        conditions.setNetworkConditions(network);
        Report.info(offline ? "Network offline" : "Network throttled");
    }
}
