package __PACKAGE__.features.home;

import io.github.andercmd.autto.core.ui.BasePage;
import io.github.andercmd.autto.core.ui.PageObject;
import org.openqa.selenium.By;

/** Page object: locators and interactions only, never assertions. */
@PageObject
public class HomePage extends BasePage {

    private static final By CONTENT = By.tagName("p");

    public HomePage open() {
        open("/");
        visible(CONTENT);
        return this;
    }

    public String pageTitle() {
        return title();
    }
}
