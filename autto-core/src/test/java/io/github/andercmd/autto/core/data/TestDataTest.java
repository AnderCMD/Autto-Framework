package io.github.andercmd.autto.core.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TestDataTest {

    record User(String username, String password) {
    }

    @Test
    void loadsEntriesAndResolvesPlaceholderDefaults() {
        User user = TestData.entry("sample/users.json", "standard", User.class);
        assertThat(user.username()).isEqualTo("standard_user");
        assertThat(user.password()).isEqualTo("fallback-pass");
    }

    @Test
    void missingDataIsReportedClearly() {
        assertThatThrownBy(() -> TestData.entry("sample/users.json", "ghost", User.class))
                .hasMessageContaining("ghost");
        assertThatThrownBy(() -> TestData.load("nope.json", User.class)).hasMessageContaining("testdata/nope.json");
        assertThatThrownBy(() -> TestData.resolve("${AUTTO_UNKNOWN_KEY}")).hasMessageContaining("AUTTO_UNKNOWN_KEY");
    }

    @Test
    void fakerIsAvailable() {
        assertThat(TestData.faker().name().firstName()).isNotBlank();
    }
}
