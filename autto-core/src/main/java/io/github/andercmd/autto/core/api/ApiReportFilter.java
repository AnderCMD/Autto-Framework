package io.github.andercmd.autto.core.api;

import io.github.andercmd.autto.core.report.Report;
import io.github.andercmd.autto.core.security.Secrets;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes every HTTP exchange to the log (one line) and to the report (request and response, collapsed). Values of
 * sensitive headers ({@code Authorization}, cookies, API keys...) and registered {@link Secrets} are masked.
 */
public class ApiReportFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiReportFilter.class);
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "proxy-authorization", "cookie",
            "set-cookie", "x-api-key", "x-auth-token");
    private static final int MAX_BODY = 20_000;

    private final boolean report;

    public ApiReportFilter(boolean report) {
        this.report = report;
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
            FilterContext context) {
        long start = System.nanoTime();
        Response result = context.next(request, response);
        long millis = (System.nanoTime() - start) / 1_000_000;
        String summary = request.getMethod() + " " + request.getURI() + " → " + result.statusCode() + " (" + millis
                + " ms)";
        LOG.info(Secrets.mask(summary));
        if (report && Report.isActive()) {
            Report.code("Request · " + request.getMethod() + " " + request.getURI(), describe(request), false);
            Report.code("Response · " + result.statusLine() + " · " + millis + " ms", describe(result), false);
        }
        return result;
    }

    static String describe(FilterableRequestSpecification request) {
        StringBuilder out = new StringBuilder(request.getMethod()).append(' ').append(request.getURI()).append('\n');
        appendHeaders(out, request.getHeaders());
        Object body = request.getBody();
        if (body != null) {
            String text = body instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8)
                    : String.valueOf(body);
            out.append('\n').append(truncate(text));
        }
        return out.toString();
    }

    static String describe(Response response) {
        StringBuilder out = new StringBuilder(response.statusLine()).append('\n');
        appendHeaders(out, response.getHeaders());
        String body = response.asPrettyString();
        if (body != null && !body.isEmpty()) {
            out.append('\n').append(truncate(body));
        }
        return out.toString();
    }

    static boolean isSensitive(String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT)) || Secrets.isSecretName(headerName);
    }

    private static void appendHeaders(StringBuilder out, Headers headers) {
        for (Header header : headers) {
            out.append(header.getName()).append(": ")
                    .append(isSensitive(header.getName()) ? Secrets.MASK : header.getValue()).append('\n');
        }
    }

    private static String truncate(String text) {
        return text.length() <= MAX_BODY ? text
                : text.substring(0, MAX_BODY) + "\n… (" + (text.length() - MAX_BODY) + " more characters)";
    }
}
