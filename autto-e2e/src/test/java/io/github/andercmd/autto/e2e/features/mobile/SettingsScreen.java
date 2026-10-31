package io.github.andercmd.autto.e2e.features.mobile;

import io.appium.java_client.AppiumBy;
import io.github.andercmd.autto.core.ui.BasePage;
import io.github.andercmd.autto.core.ui.PageObject;
import org.openqa.selenium.By;

/**
 * Screen object of a native app. It extends {@link BasePage} exactly like a web page: the same explicit waits and
 * interactions work, only the locators change ({@link AppiumBy}: accessibility id, resource id, UiAutomator...).
 */
@PageObject
public class SettingsScreen extends BasePage {

    private static final By SEARCH = AppiumBy.id("com.android.settings:id/search_action_bar");

    public boolean isSearchBarDisplayed() {
        return isDisplayed(SEARCH, config().timeouts().explicit());
    }
}
