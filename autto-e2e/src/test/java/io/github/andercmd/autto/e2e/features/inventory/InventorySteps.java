package io.github.andercmd.autto.e2e.features.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.andercmd.autto.core.report.Report;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class InventorySteps {

    private final InventoryPage inventory;

    public InventorySteps(InventoryPage inventory) {
        this.inventory = inventory;
    }

    @When("the customer sorts the products by {string}")
    public void theCustomerSortsTheProductsBy(String option) {
        inventory.sortBy(option);
    }

    @When("the customer adds the following products to the cart:")
    public void theCustomerAddsTheFollowingProducts(List<String> products) {
        products.forEach(inventory::addToCart);
    }

    @Then("the products are sorted by price in ascending order")
    public void theProductsAreSortedByPriceAscending() {
        List<BigDecimal> prices = inventory.productPrices();
        Report.info("Prices shown: " + prices);
        assertThat(prices).isNotEmpty().isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Then("the products are sorted by name in descending order")
    public void theProductsAreSortedByNameDescending() {
        List<String> names = inventory.productNames();
        Report.info("Names shown: " + names);
        assertThat(names).isNotEmpty().isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Then("the cart badge shows {int} products")
    public void theCartBadgeShows(int expected) {
        assertThat(inventory.cartCount()).isEqualTo(expected);
    }

    @Then("every product shows its own image")
    public void everyProductShowsItsOwnImage() {
        List<String> images = inventory.productImageSources();
        assertThat(images).as("product image sources").doesNotHaveDuplicates();
    }
}
