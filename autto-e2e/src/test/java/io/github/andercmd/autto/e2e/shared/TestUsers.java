package io.github.andercmd.autto.e2e.shared;

import io.github.andercmd.autto.core.security.Credentials;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Test users declared in {@code application.yml} under {@code test-data.users.<alias>}. Passwords are references to
 * secrets ({@code ${SAUCE_PASSWORD}}) resolved from the {@code .env} file locally or from CI secrets, so they never
 * live in the repository.
 */
@Component
public class TestUsers {

    private static final String PREFIX = "test-data.users.";

    private final Environment environment;

    public TestUsers(Environment environment) {
        this.environment = environment;
    }

    public Credentials get(String alias) {
        String username = environment.getProperty(PREFIX + alias + ".username");
        if (username == null) {
            throw new IllegalArgumentException("Unknown test user '" + alias + "'. Declare it in application.yml under "
                    + PREFIX + alias);
        }
        String password;
        try {
            password = environment.getProperty(PREFIX + alias + ".password");
        } catch (IllegalArgumentException e) {
            throw missingSecret(alias, e.getMessage());
        }
        if (password == null || password.isBlank()) {
            throw missingSecret(alias, "the secret is empty");
        }
        return new Credentials(username, password);
    }

    private static IllegalStateException missingSecret(String alias, String cause) {
        return new IllegalStateException("The password of test user '" + alias + "' is not available ("
                + cause + "). Copy .env.example to .env and fill in the secrets, or define them as environment "
                + "variables / CI secrets.");
    }
}
