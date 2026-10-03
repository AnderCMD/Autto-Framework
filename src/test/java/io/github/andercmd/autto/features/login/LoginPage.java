package io.github.andercmd.autto.features.login;

import io.github.andercmd.autto.core.ui.BasePage;
import org.openqa.selenium.By;

/** Login screen of the store. */
public class LoginPage extends BasePage {

    private static final By USERNAME = By.id("user-name");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN_BUTTON = By.id("login-button");
    private static final By ERROR = By.cssSelector("[data-test='error']");

    public LoginPage open() {
        open("/");
        visible(LOGIN_BUTTON);
        return this;
    }

    public void loginAs(Credentials credentials) {
        login(credentials.username(), credentials.password());
    }

    public void login(String username, String password) {
        log.info("Logging in as '{}'", username);
        type(USERNAME, username);
        type(PASSWORD, password);
        click(LOGIN_BUTTON);
    }

    public String errorMessage() {
        return text(ERROR);
    }
}
