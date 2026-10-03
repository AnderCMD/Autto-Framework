package io.github.andercmd.autto.core.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TestDataTest {

    record User(String username, String password) {
    }

    @Test
    void loadsEntriesAndResolvesPlaceholderDefaults() {
        User user = TestData.entry("login/users.json", "standard", User.class);
        assertThat(user.username()).isEqualTo("standard_user");
        assertThat(user.password()).isEqualTo("secret_sauce");
    }

    @Test
    void missingDataIsReportedClearly() {
        assertThatThrownBy(() -> TestData.entry("login/users.json", "ghost", User.class)).hasMessageContaining("ghost");
        assertThatThrownBy(() -> TestData.load("nope.json", User.class)).hasMessageContaining("testdata/nope.json");
        assertThatThrownBy(() -> TestData.resolve("${autto.unknown.key}")).hasMessageContaining("AUTTO_AUTTO_UNKNOWN_KEY");
    }

    @Test
    void fakerIsAvailable() {
        assertThat(TestData.faker().name().firstName()).isNotBlank();
    }
}
