package io.github.andercmd.autto.e2e.features.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.andercmd.autto.core.context.ScenarioContext;
import io.github.andercmd.autto.core.report.Report;
import io.github.andercmd.autto.e2e.features.inventory.InventoryPage;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class CheckoutSteps {

    private static final String CART_PRICES = "checkout.cartPrices";

    private final InventoryPage inventory;
    private final CartPage cart;
    private final CheckoutPage checkout;
    private final ScenarioContext context;

    public CheckoutSteps(InventoryPage inventory, CartPage cart, CheckoutPage checkout, ScenarioContext context) {
        this.inventory = inventory;
        this.cart = cart;
        this.checkout = checkout;
        this.context = context;
    }

    @Given("the customer has added {string} to the cart")
    public void theCustomerHasAddedToTheCart(String product) {
        cartPrices().put(product, inventory.priceOf(product));
        inventory.addToCart(product);
    }

    @When("the customer checks out with random customer information")
    public void theCustomerChecksOutWithRandomCustomerInformation() {
        Customer customer = Customer.random();
        Report.table("Customer", Map.of(
                "First name", customer.firstName(),
                "Last name", customer.lastName(),
                "Postal code", customer.postalCode()));
        goToCheckout();
        checkout.fillInformation(customer);
        checkout.continueToOverview();
    }

    @When("the customer checks out without filling the form")
    public void theCustomerChecksOutWithoutFillingTheForm() {
        goToCheckout();
        checkout.continueToOverview();
    }

    @Then("the order summary total equals the sum of the item prices plus tax")
    public void theOrderSummaryTotalIsCorrect() {
        BigDecimal expectedItems = cartPrices().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Report.table("Order summary", Map.of(
                "Items (expected)", expectedItems,
                "Items (shown)", checkout.itemTotal(),
                "Tax", checkout.tax(),
                "Total", checkout.total()));
        assertThat(checkout.itemTotal()).isEqualByComparingTo(expectedItems);
        assertThat(checkout.total()).isEqualByComparingTo(checkout.itemTotal().add(checkout.tax()));
        Report.screenshot("Order overview");
    }

    @When("the customer finishes the order")
    public void theCustomerFinishesTheOrder() {
        checkout.finish();
    }

    @Then("the confirmation message {string} is displayed")
    public void theConfirmationMessageIsDisplayed(String message) {
        assertThat(checkout.confirmationMessage()).isEqualTo(message);
        Report.screenshot("Order confirmation");
    }

    @Then("the checkout error {string} is shown")
    public void theCheckoutErrorIsShown(String message) {
        assertThat(checkout.errorMessage()).isEqualTo(message);
    }

    private void goToCheckout() {
        inventory.openCart();
        assertThat(cart.productNames()).containsExactlyInAnyOrderElementsOf(cartPrices().keySet());
        cart.checkout();
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> cartPrices() {
        if (!context.contains(CART_PRICES)) {
            context.put(CART_PRICES, new LinkedHashMap<String, BigDecimal>());
        }
        return context.get(CART_PRICES, Map.class);
    }
}
