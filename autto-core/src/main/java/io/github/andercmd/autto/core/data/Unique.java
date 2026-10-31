package io.github.andercmd.autto.core.data;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Values that are unique across scenarios, threads and runs, so parallel scenarios never collide on shared
 * environments (two scenarios registering the same e-mail, the same order reference...).
 *
 * <pre>{@code
 * String email = Unique.email();          // autto+k3j9x2-1@example.test
 * String user = Unique.of("customer");    // customer-k3j9x2-2
 * }</pre>
 */
public final class Unique {

    private static final String RUN = Long.toString(System.currentTimeMillis() % 1_000_000_000L, 36)
            + Long.toString(ProcessHandle.current().pid() % 1296, 36);
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private Unique() {
    }

    /** {@code <prefix>-<run>-<sequence>}, lower-case and safe for user names, references and file names. */
    public static String of(String prefix) {
        return prefix.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-") + "-" + RUN + "-"
                + SEQUENCE.incrementAndGet();
    }

    /** A unique e-mail address in the reserved {@code example.test} domain. */
    public static String email() {
        return email("autto");
    }

    public static String email(String localPart) {
        return of(localPart).replace('-', '.') + "@example.test";
    }

    /** A unique number, useful as an order or reference id. */
    public static long number() {
        return Long.parseLong(Long.toString(System.nanoTime() % 100_000_000L) + SEQUENCE.incrementAndGet() % 10);
    }
}
