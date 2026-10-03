package io.github.andercmd.autto.features.login;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.andercmd.autto.core.report.Report;
import io.github.andercmd.autto.features.inventory.InventoryPage;

public class LoginSteps {

    private final LoginPage loginPage;
    private final InventoryPage inventoryPage;

    public LoginSteps(LoginPage loginPage, InventoryPage inventoryPage) {
        this.loginPage = loginPage;
        this.inventoryPage = inventoryPage;
    }

    @Given("the customer is on the login page")
    public void theCustomerIsOnTheLoginPage() {
        loginPage.open();
    }

    @Given("the customer is logged in as {string}")
    public void theCustomerIsLoggedInAs(String alias) {
        loginPage.open().loginAs(Credentials.of(alias));
        assertThat(inventoryPage.isDisplayed()).as("catalog visible after login").isTrue();
    }

    @When("the customer logs in as {string}")
    public void theCustomerLogsInAs(String alias) {
        loginPage.loginAs(Credentials.of(alias));
    }

    @When("the customer logs in with username {string} and password {string}")
    public void theCustomerLogsInWith(String username, String password) {
        loginPage.login(username, password);
    }

    @Then("the products catalog is displayed")
    public void theProductsCatalogIsDisplayed() {
        assertThat(inventoryPage.isDisplayed()).as("catalog visible").isTrue();
        assertThat(inventoryPage.heading()).isEqualTo("Products");
        Report.screenshot("Products catalog");
    }

    @Then("the login error {string} is shown")
    public void theLoginErrorIsShown(String message) {
        assertThat(loginPage.errorMessage()).isEqualTo(message);
    }
}
