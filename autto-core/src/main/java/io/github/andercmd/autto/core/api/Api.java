package io.github.andercmd.autto.core.api;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

/**
 * Pre-configured REST Assured entry point for API tests and for preparing data through APIs in UI scenarios.
 *
 * <p>Every request gets {@code autto.api.base-url}, the default headers {@code autto.api.headers.*}, the timeouts
 * and, unless {@code autto.api.report=false}, a filter that writes the request and the response to the report and
 * the log with secrets masked. Inject it (it is a Spring bean) or use {@link #get()}.
 *
 * <pre>{@code
 * public OrderSteps(Api api) { this.api = api; }
 *
 * Response response = api.request()
 *         .auth().oauth2(token)
 *         .body(order)
 *         .post("/orders");
 * assertThat(response.statusCode()).isEqualTo(201);
 * }</pre>
 */
public class Api {

    private final AuttoProperties.Api config;
    private final Map<String, String> headers;
    private final RestAssuredConfig restConfig;

    public Api(AuttoSettings settings) {
        this.config = settings.properties().api();
        this.headers = Map.copyOf(settings.apiHeaders());
        this.restConfig = RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", Math.toIntExact(config.connectTimeout().toMillis()))
                .setParam("http.socket.timeout", Math.toIntExact(config.readTimeout().toMillis())));
    }

    /** Client built from the global configuration, for code that does not use dependency injection. */
    public static Api get() {
        return new Api(AuttoSettings.get());
    }

    /** A new request with the framework defaults applied. */
    public RequestSpecification request() {
        RequestSpecification request = RestAssured.given().config(restConfig);
        if (config.baseUrl() != null) {
            request.baseUri(config.baseUrl());
        }
        if (config.relaxedHttps()) {
            request.relaxedHTTPSValidation();
        }
        if (!headers.isEmpty()) {
            request.headers(headers);
        }
        return request.filter(new ApiReportFilter(config.report()));
    }

    /** Base URL of the API ({@code autto.api.base-url}), or {@code null} when every request uses absolute URLs. */
    public String baseUrl() {
        return config.baseUrl();
    }
}
