package io.github.andercmd.autto.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpTest {

    /** RFC 6238 appendix B: ASCII secret "12345678901234567890", SHA-1. */
    private static final String SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void matchesTheRfc6238TestVectors() {
        assertThat(Totp.at(SECRET, Instant.ofEpochSecond(59), 30, 8)).isEqualTo("94287082");
        assertThat(Totp.at(SECRET, Instant.ofEpochSecond(1111111109L), 30, 8)).isEqualTo("07081804");
        assertThat(Totp.at(SECRET, Instant.ofEpochSecond(59), 30, 6)).isEqualTo("287082");
    }

    @Test
    void acceptsSecretsWithSpacesAndLowerCase() {
        assertThat(Totp.at("gezd gnbv gy3t qojq gezd gnbv gy3t qojq", Instant.ofEpochSecond(59), 30, 6))
                .isEqualTo("287082");
    }

    @Test
    void rejectsInvalidBase32() {
        assertThatThrownBy(() -> Totp.now("not-base32-1!")).isInstanceOf(IllegalArgumentException.class);
    }
}
