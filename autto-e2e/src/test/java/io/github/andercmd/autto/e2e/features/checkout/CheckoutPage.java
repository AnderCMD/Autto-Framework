package io.github.andercmd.autto.e2e.features.checkout;

import io.github.andercmd.autto.core.ui.BasePage;
import io.github.andercmd.autto.core.ui.PageObject;
import java.math.BigDecimal;
import org.openqa.selenium.By;

/** Checkout steps: information form, overview and confirmation. */
@PageObject
public class CheckoutPage extends BasePage {

    private static final By FIRST_NAME = By.id("first-name");
    private static final By LAST_NAME = By.id("last-name");
    private static final By POSTAL_CODE = By.id("postal-code");
    private static final By CONTINUE = By.id("continue");
    private static final By ERROR = By.cssSelector("[data-test='error']");
    private static final By SUBTOTAL = By.cssSelector("[data-test='subtotal-label']");
    private static final By TAX = By.cssSelector("[data-test='tax-label']");
    private static final By TOTAL = By.cssSelector("[data-test='total-label']");
    private static final By FINISH = By.id("finish");
    private static final By COMPLETE_HEADER = By.cssSelector("[data-test='complete-header']");

    public void fillInformation(Customer customer) {
        log.info("Checkout information: {}", customer);
        type(FIRST_NAME, customer.firstName());
        type(LAST_NAME, customer.lastName());
        type(POSTAL_CODE, customer.postalCode());
    }

    public void continueToOverview() {
        click(CONTINUE);
    }

    public String errorMessage() {
        return text(ERROR);
    }

    public BigDecimal itemTotal() {
        return money(text(SUBTOTAL));
    }

    public BigDecimal tax() {
        return money(text(TAX));
    }

    public BigDecimal total() {
        return money(text(TOTAL));
    }

    public void finish() {
        click(FINISH);
    }

    public String confirmationMessage() {
        return text(COMPLETE_HEADER);
    }

    private static BigDecimal money(String text) {
        return new BigDecimal(text.replaceAll("[^0-9.]", ""));
    }
}
