package io.github.andercmd.autto.e2e.features.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.andercmd.autto.core.api.Api;
import io.github.andercmd.autto.core.context.ScenarioContext;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;

/**
 * API steps: the {@link Api} client applies {@code autto.api.*} and writes every request and response to the report;
 * {@link SoftAssertions} are verified automatically when the scenario ends.
 */
public class StorefrontApiSteps {

    private static final String RESPONSE = "api.response";

    private final Api api;
    private final ScenarioContext context;
    private final SoftAssertions softly;

    public StorefrontApiSteps(Api api, ScenarioContext context, SoftAssertions softly) {
        this.api = api;
        this.context = context;
        this.softly = softly;
    }

    @When("the client requests {string}")
    public void theClientRequests(String path) {
        context.put(RESPONSE, api.request().get(path));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(response().statusCode()).isEqualTo(status);
    }

    @Then("the response is an HTML page titled {string}")
    public void theResponseIsAnHtmlPageTitled(String title) {
        Response response = response();
        softly.assertThat(response.contentType()).as("content type").startsWith("text/html");
        softly.assertThat(response.body().asString()).as("page title").contains("<title>" + title + "</title>");
        softly.assertThat(response.time()).as("response time (ms)").isLessThan(10_000L);
    }

    private Response response() {
        return context.get(RESPONSE, Response.class);
    }
}
