package io.github.andercmd.autto.e2e.features.checkout;

import io.github.andercmd.autto.core.ui.BasePage;
import io.github.andercmd.autto.core.ui.PageObject;
import java.util.List;
import org.openqa.selenium.By;

/** Shopping cart. */
@PageObject
public class CartPage extends BasePage {

    private static final By ITEM_NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By CHECKOUT = By.id("checkout");

    public List<String> productNames() {
        visible(CHECKOUT);
        return texts(ITEM_NAME);
    }

    public void checkout() {
        click(CHECKOUT);
    }
}
