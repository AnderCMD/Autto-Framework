package io.github.andercmd.autto.e2e.features.quality;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.andercmd.autto.core.driver.DriverManager;
import io.github.andercmd.autto.core.network.NetworkMock;
import io.github.andercmd.autto.core.perf.WebPerformance;
import io.github.andercmd.autto.core.visual.VisualRegression;
import java.time.Duration;

public class QualitySteps {

    private final NetworkMock network;
    private Duration elapsed = Duration.ZERO;

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

    @Given("the response of {string} is stubbed with a page titled {string}")
    public void theResponseIsStubbed(String host, String title) {
        network.stub(host, 200, "text/html", "<html><head><title>" + title + "</title></head><body></body></html>");
    }

    @Given("requests to {string} are delayed by {int} seconds")
    public void requestsAreDelayed(String host, int seconds) {
        network.delay(host, Duration.ofSeconds(seconds));
    }

    @When("the browser opens {string}")
    public void theBrowserOpens(String url) {
        long start = System.nanoTime();
        DriverManager.driver().get(url);
        elapsed = Duration.ofNanos(System.nanoTime() - start);
    }

    @Then("the browser title is {string}")
    public void theBrowserTitleIs(String title) {
        assertThat(DriverManager.driver().getTitle()).isEqualTo(title);
    }

    @Then("opening it took at least {int} seconds")
    public void openingItTookAtLeast(int seconds) {
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofSeconds(seconds));
    }

    @Then("the page looks like the {string} baseline")
    public void thePageLooksLikeTheBaseline(String name) {
        VisualRegression.assertMatches(name);
    }
}
