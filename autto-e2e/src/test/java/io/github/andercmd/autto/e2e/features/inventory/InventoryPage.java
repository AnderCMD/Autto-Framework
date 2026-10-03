package io.github.andercmd.autto.e2e.features.inventory;

import io.github.andercmd.autto.core.ui.BasePage;
import io.github.andercmd.autto.core.ui.PageObject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Product catalog (inventory) of the store. */
@PageObject
public class InventoryPage extends BasePage {

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By ITEM = By.cssSelector("[data-test='inventory-item']");
    private static final By ITEM_NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By ITEM_PRICE = By.cssSelector("[data-test='inventory-item-price']");
    private static final By ITEM_IMAGE = By.cssSelector("img.inventory_item_img");
    private static final By SORT = By.cssSelector("[data-test='product-sort-container']");
    private static final By CART_BADGE = By.cssSelector("[data-test='shopping-cart-badge']");
    private static final By CART_LINK = By.cssSelector("[data-test='shopping-cart-link']");

    public boolean isDisplayed() {
        return isDisplayed(ITEM, Duration.ofSeconds(10));
    }

    public String heading() {
        return text(TITLE);
    }

    public void sortBy(String visibleOption) {
        log.info("Sorting products by '{}'", visibleOption);
        selectByText(SORT, visibleOption);
    }

    public List<String> productNames() {
        allVisible(ITEM);
        return texts(ITEM_NAME);
    }

    public List<BigDecimal> productPrices() {
        allVisible(ITEM);
        return texts(ITEM_PRICE).stream().map(InventoryPage::money).toList();
    }

    public List<String> productImageSources() {
        allVisible(ITEM);
        return driver().findElements(ITEM_IMAGE).stream().map(img -> img.getDomAttribute("src")).toList();
    }

    public void addToCart(String productName) {
        log.info("Adding '{}' to the cart", productName);
        click(By.id("add-to-cart-" + slug(productName)));
    }

    public BigDecimal priceOf(String productName) {
        for (WebElement item : driver().findElements(ITEM)) {
            if (item.findElement(ITEM_NAME).getText().trim().equals(productName)) {
                return money(item.findElement(ITEM_PRICE).getText());
            }
        }
        throw new IllegalArgumentException("Product not found in the catalog: " + productName);
    }

    public int cartCount() {
        return isDisplayed(CART_BADGE) ? Integer.parseInt(text(CART_BADGE)) : 0;
    }

    public void openCart() {
        click(CART_LINK);
    }

    static BigDecimal money(String text) {
        return new BigDecimal(text.replaceAll("[^0-9.]", ""));
    }

    private static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
