package io.github.andercmd.autto.e2e.features.quality;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.github.andercmd.autto.core.network.NetworkMock;
import io.github.andercmd.autto.core.perf.WebPerformance;
import io.github.andercmd.autto.core.visual.VisualRegression;

public class QualitySteps {

    private final NetworkMock network;

    public QualitySteps(NetworkMock network) {
        this.network = network;
    }

    @Then("the page is within the performance budget")
    public void thePageIsWithinThePerformanceBudget() {
        WebPerformance.measure().assertWithinBudget();
    }

    @Given("the third party {string} is blocked")
    public void theThirdPartyIsBlocked(String host) {
        network.block(host);
    }

    @Then("the page looks like the {string} baseline")
    public void thePageLooksLikeTheBaseline(String name) {
        VisualRegression.assertMatches(name);
    }
}
