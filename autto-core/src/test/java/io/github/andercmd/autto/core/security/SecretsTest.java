package io.github.andercmd.autto.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SecretsTest {

    @AfterEach
    void clear() {
        Secrets.clear();
    }

    @Test
    void registersOnlySecretLookingNames() {
        Secrets.registerAll(Map.of(
                "SAUCE_PASSWORD", "secret_sauce",
                "BROWSERSTACK_ACCESS_KEY", "abcd1234",
                "api.token", "tok-999",
                "AUTTO_BROWSER_NAME", "chrome"));

        assertThat(Secrets.mask("login secret_sauce with abcd1234 and tok-999 on chrome"))
                .isEqualTo("login ****** with ****** and ****** on chrome");
    }

    @Test
    void secretNameDetection() {
        assertThat(Secrets.isSecretName("SAUCE_PASSWORD")).isTrue();
        assertThat(Secrets.isSecretName("db.password")).isTrue();
        assertThat(Secrets.isSecretName("GITHUB_TOKEN")).isTrue();
        assertThat(Secrets.isSecretName("AWS_SECRET_ACCESS_KEY")).isTrue();
        assertThat(Secrets.isSecretName("BROWSERSTACK_ACCESS_KEY")).isTrue();
        assertThat(Secrets.isSecretName("stripeApiKey")).isTrue();
        assertThat(Secrets.isSecretName("CLIENT_CREDENTIALS")).isTrue();

        assertThat(Secrets.isSecretName("PWD")).as("shell working directory").isFalse();
        assertThat(Secrets.isSecretName("MAX_THINKING_TOKENS")).as("plural, not a token").isFalse();
        assertThat(Secrets.isSecretName("SESSION_TOKEN_FILE")).as("reference to a secret").isFalse();
        assertThat(Secrets.isSecretName("SSH_KEY_PATH")).isFalse();
        assertThat(Secrets.isSecretName("AUTTO_BROWSER_NAME")).isFalse();
        assertThat(Secrets.isSecretName("monkey")).isFalse();
    }

    @Test
    void ignoresVeryShortValuesAndNulls() {
        Secrets.register("abc");
        Secrets.register(null);
        assertThat(Secrets.mask("abc")).isEqualTo("abc");
        assertThat(Secrets.mask(null)).isNull();
    }

    @Test
    void credentialsNeverPrintThePassword() {
        Credentials credentials = new Credentials("jane", "Sup3rS3cret!");
        assertThat(credentials.toString()).doesNotContain("Sup3rS3cret!").contains("******");
        assertThat(Secrets.mask("typed Sup3rS3cret!")).isEqualTo("typed ******");
    }
}
