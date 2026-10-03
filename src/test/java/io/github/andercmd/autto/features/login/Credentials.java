package io.github.andercmd.autto.features.login;

import io.github.andercmd.autto.core.data.TestData;

/** User credentials, loaded from {@code testdata/login/users.json}. */
public record Credentials(String username, String password) {

    public static Credentials of(String alias) {
        return TestData.entry("login/users.json", alias, Credentials.class);
    }
}
