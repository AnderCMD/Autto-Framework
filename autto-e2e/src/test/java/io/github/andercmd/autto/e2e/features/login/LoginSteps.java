package io.github.andercmd.autto.e2e.features.login;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.andercmd.autto.core.report.Report;
import io.github.andercmd.autto.e2e.features.inventory.InventoryPage;
import io.github.andercmd.autto.e2e.shared.TestUsers;

public class LoginSteps {

    private final LoginPage loginPage;
    private final InventoryPage inventoryPage;
    private final TestUsers users;

    public LoginSteps(LoginPage loginPage, InventoryPage inventoryPage, TestUsers users) {
        this.loginPage = loginPage;
        this.inventoryPage = inventoryPage;
        this.users = users;
    }

    @Given("the customer is on the login page")
    public void theCustomerIsOnTheLoginPage() {
        loginPage.open();
    }

    @Given("the customer is logged in as {string}")
    public void theCustomerIsLoggedInAs(String alias) {
        loginPage.open().loginAs(users.get(alias));
        assertThat(inventoryPage.isDisplayed()).as("catalog visible after login").isTrue();
    }

    @When("the customer logs in as {string}")
    public void theCustomerLogsInAs(String alias) {
        loginPage.loginAs(users.get(alias));
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
