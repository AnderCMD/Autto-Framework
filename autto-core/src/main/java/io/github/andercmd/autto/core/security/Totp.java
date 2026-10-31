package io.github.andercmd.autto.core.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Time-based one-time passwords (RFC 6238) for scenarios that log in with two-factor authentication. Keep the
 * Base32 secret of the test account in {@code .env} or a CI secret.
 *
 * <pre>{@code
 * String code = Totp.now("JBSWY3DPEHPK3PXP");
 * }</pre>
 */
public final class Totp {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Totp() {
    }

    /** The 6 digit code valid right now (30 second step, HMAC-SHA1: what authenticator apps use). */
    public static String now(String base32Secret) {
        return at(base32Secret, Instant.now(), 30, 6);
    }

    public static String at(String base32Secret, Instant time, int stepSeconds, int digits) {
        long counter = time.getEpochSecond() / stepSeconds;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decode(base32Secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            int modulo = 1;
            for (int i = 0; i < digits; i++) {
                modulo *= 10;
            }
            return String.format("%0" + digits + "d", binary % modulo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TOTP could not be computed", e);
        }
    }

    static byte[] decode(String base32) {
        String clean = base32.replaceAll("[\\s=-]", "").toUpperCase(java.util.Locale.ROOT);
        ByteBuffer out = ByteBuffer.allocate(clean.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : clean.toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("The TOTP secret is not valid Base32");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) (buffer >> (bits - 8)));
                bits -= 8;
            }
        }
        return out.array();
    }
}
