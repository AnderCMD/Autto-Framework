package io.github.andercmd.autto.core.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the local {@code .env} file (git-ignored) with developer secrets and overrides.
 *
 * <p>Location: {@code -Dautto.dotenv.path} / {@code AUTTO_DOTENV_PATH}, otherwise the first {@code .env} found in
 * the working directory or up to three parent folders (so it works from the repository root and from modules).
 *
 * <p>Precedence: real environment variables and system properties always win over {@code .env}, so CI secrets
 * cannot be shadowed by a stale local file.
 */
public final class DotEnv {

    public static final String FILE_NAME = ".env";
    public static final String PATH_PROPERTY = "autto.dotenv.path";
    public static final String PATH_VARIABLE = "AUTTO_DOTENV_PATH";
    private static final int MAX_PARENT_LEVELS = 3;
    private static final Pattern ENTRY = Pattern.compile("^(?:export\\s+)?([A-Za-z_][A-Za-z0-9_.-]*)\\s*=(.*)$");

    private DotEnv() {
    }

    /** Finds the {@code .env} file to use, if any. */
    public static Optional<Path> locate(Map<String, String> environment, Map<String, String> systemProperties) {
        String explicit = Optional.ofNullable(systemProperties.get(PATH_PROPERTY))
                .orElse(environment.get(PATH_VARIABLE));
        if (explicit != null && !explicit.isBlank()) {
            Path path = Path.of(explicit.trim());
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("The .env file configured in " + PATH_PROPERTY + " / " + PATH_VARIABLE
                        + " does not exist: " + path.toAbsolutePath());
            }
            return Optional.of(path);
        }
        Path dir = Path.of(systemProperties.getOrDefault("user.dir", ".")).toAbsolutePath();
        for (int level = 0; dir != null && level <= MAX_PARENT_LEVELS; level++) {
            Path candidate = dir.resolve(FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
            dir = dir.getParent();
        }
        return Optional.empty();
    }

    /**
     * Parses a {@code .env} file. Supported syntax:
     *
     * <pre>
     * # comment
     * KEY=value                 unquoted (an inline " #comment" is removed)
     * export KEY=value          "export" prefix (shell compatible)
     * KEY='literal $value'      single quotes: taken literally
     * KEY="line1\nline2"        double quotes: \n, \t, \" and \\ escapes, may span several lines
     * </pre>
     */
    public static Map<String, String> read(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        }
        return parse(lines, file.toString());
    }

    static Map<String, String> parse(List<String> lines, String source) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = ENTRY.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalStateException("Malformed line " + (i + 1) + " in " + source
                        + " (expected KEY=value)");
            }
            String key = matcher.group(1);
            String raw = matcher.group(2).strip();
            String value;
            if (raw.startsWith("'")) {
                int end = raw.indexOf('\'', 1);
                if (end < 0) {
                    throw new IllegalStateException("Unterminated single quote at line " + (i + 1) + " in " + source);
                }
                value = raw.substring(1, end);
            } else if (raw.startsWith("\"")) {
                StringBuilder text = new StringBuilder(raw.substring(1));
                int closing = closingQuote(text);
                while (closing < 0 && i + 1 < lines.size()) {
                    text.append('\n').append(lines.get(++i));
                    closing = closingQuote(text);
                }
                if (closing < 0) {
                    throw new IllegalStateException("Unterminated double quote for " + key + " in " + source);
                }
                value = unescape(text.substring(0, closing));
            } else {
                int comment = raw.indexOf(" #");
                value = (comment >= 0 ? raw.substring(0, comment) : raw).strip();
            }
            values.put(key, value);
        }
        return values;
    }

    private static int closingQuote(CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                out.append(switch (next) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    default -> next;
                });
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
