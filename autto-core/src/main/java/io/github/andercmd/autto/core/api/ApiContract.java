package io.github.andercmd.autto.core.api;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import io.restassured.response.Response;

/**
 * Contract testing of API responses against JSON Schema files kept in {@code src/test/resources/contracts}
 * (export them from your OpenAPI document or write them by hand).
 *
 * <pre>{@code
 * Response response = api.request().get("/orders/42");
 * ApiContract.assertMatches(response, "order.json");
 * }</pre>
 */
public final class ApiContract {

    private static final String ROOT = "contracts/";

    private ApiContract() {
    }

    /** Fails with the schema violations when the body does not match {@code contracts/<schema>}. */
    public static void assertMatches(Response response, String schema) {
        response.then().body(matchesJsonSchemaInClasspath(ROOT + schema));
    }
}
