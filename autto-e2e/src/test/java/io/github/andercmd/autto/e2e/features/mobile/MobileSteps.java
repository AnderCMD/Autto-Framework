package io.github.andercmd.autto.e2e.features.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class MobileSteps {

    private final SettingsScreen settings;

    public MobileSteps(SettingsScreen settings) {
        this.settings = settings;
    }

    @Given("the Settings app is open")
    public void theSettingsAppIsOpen() {
        // The app is started by the session itself (appium:appPackage / appium:appActivity capabilities).
        assertThat(settings.isSearchBarDisplayed()).as("Settings is in the foreground").isTrue();
    }

    @Then("the Settings search bar is displayed")
    public void theSettingsSearchBarIsDisplayed() {
        assertThat(settings.isSearchBarDisplayed()).isTrue();
    }
}
