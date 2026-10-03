package io.github.andercmd.autto.core.security;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Registry of secret values that must never appear in logs or reports.
 *
 * <p>Every environment variable, {@code .env} entry or system property whose <b>name</b> looks like a secret
 * (see {@link #isSecretName(String)}) is registered automatically when the configuration is loaded. The Extent
 * report, the {@code Report} API and the log
 * pattern {@code %maskedMsg} replace those values with {@value #MASK}.
 */
public final class Secrets {

    public static final String MASK = "******";

    private static final Set<String> SECRET_WORDS = Set.of("password", "passwd", "passphrase", "secret", "token",
            "credential", "credentials", "apikey");
    private static final Set<String> KEY_QUALIFIERS = Set.of("api", "access", "private", "secret", "auth", "signing");
    /** Names ending like this point to a secret (a file, a URL...) but are not the secret itself. */
    private static final Set<String> REFERENCE_SUFFIXES = Set.of("file", "path", "dir", "url", "uri", "home",
            "location", "name", "id", "type", "enabled");
    private static final Pattern SEPARATORS = Pattern.compile("[_.\\-\\s]+|(?<=[a-z0-9])(?=[A-Z])");
    private static final int MIN_LENGTH = 4;
    private static final Set<String> VALUES = ConcurrentHashMap.newKeySet();

    private Secrets() {
    }

    /**
     * Whether a configuration key or variable name denotes a secret: one of its words is {@code password},
     * {@code secret}, {@code token}, {@code credential}... or a qualified {@code key} ({@code API_KEY},
     * {@code accessKey}, {@code PRIVATE_KEY}). Names that only reference a secret ({@code TOKEN_FILE},
     * {@code PASSWORD_URL}) are excluded, as well as plurals such as {@code MAX_TOKENS}.
     */
    public static boolean isSecretName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        List<String> words = Arrays.stream(SEPARATORS.split(name.trim()))
                .filter(w -> !w.isEmpty())
                .map(w -> w.toLowerCase(Locale.ROOT))
                .toList();
        if (words.isEmpty() || REFERENCE_SUFFIXES.contains(words.getLast())) {
            return false;
        }
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (SECRET_WORDS.contains(word)) {
                return true;
            }
            if (word.equals("key") && i > 0 && KEY_QUALIFIERS.contains(words.get(i - 1))) {
                return true;
            }
        }
        return false;
    }

    /** Registers a value to be masked. Values shorter than 4 characters are ignored (too many false positives). */
    public static void register(String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= MIN_LENGTH && !trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) {
            VALUES.add(trimmed);
        }
    }

    /** Registers every value whose key looks like a secret name. */
    public static void registerAll(Map<String, String> entries) {
        entries.forEach((name, value) -> {
            if (isSecretName(name)) {
                register(value);
            }
        });
    }

    /** Replaces every registered secret contained in {@code text}. */
    public static String mask(String text) {
        if (text == null || text.isEmpty() || VALUES.isEmpty()) {
            return text;
        }
        String result = text;
        // longest first, so a secret containing another one is fully masked
        for (String secret : VALUES.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            if (result.contains(secret)) {
                result = result.replace(secret, MASK);
            }
        }
        return result;
    }

    /** Removes every registered value. Intended for tests. */
    public static void clear() {
        VALUES.clear();
    }
}
