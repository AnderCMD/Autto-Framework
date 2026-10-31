package __PACKAGE__.features.home;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class HomeSteps {

    private final HomePage home;

    public HomeSteps(HomePage home) {
        this.home = home;
    }

    @Given("the visitor opens the home page")
    public void theVisitorOpensTheHomePage() {
        home.open();
    }

    @Then("the page title is {string}")
    public void thePageTitleIs(String title) {
        assertThat(home.pageTitle()).isEqualTo(title);
    }
}
