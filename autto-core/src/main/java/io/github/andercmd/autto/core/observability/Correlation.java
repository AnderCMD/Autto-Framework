package io.github.andercmd.autto.core.observability;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Correlation id of the running scenario. It is placed in the logging context ({@code %X{correlationId}}) and sent
 * as the {@value #HEADER} header of every API request, so a failing scenario can be traced through the logs of the
 * application under test.
 */
public final class Correlation {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER = "X-Correlation-Id";

    private Correlation() {
    }

    /** Starts a new correlation id for the current thread and returns it. */
    public static String begin() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_KEY, id);
        return id;
    }

    public static void end() {
        MDC.remove(MDC_KEY);
    }
}
