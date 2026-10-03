package io.github.andercmd.autto.core.security;

/**
 * Username and password of a test user. The password is never printed by {@link #toString()} and is registered
 * in {@link Secrets} so it is masked in logs and reports.
 */
public record Credentials(String username, String password) {

    public Credentials {
        Secrets.register(password);
    }

    @Override
    public String toString() {
        return "Credentials[username=" + username + ", password=" + Secrets.MASK + "]";
    }
}
