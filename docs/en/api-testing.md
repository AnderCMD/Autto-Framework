# API testing

[← Back to README](../../README.md) · [Español](../es/pruebas-api.md)

Autto ships [REST Assured](https://rest-assured.io) behind the `Api` bean. Use it to test services directly, to
prepare data for UI scenarios in milliseconds instead of clicking through forms, and to verify back-end state.

## Configuration

```yaml
autto:
  api:
    base-url: https://api.qa.example.com      # per environment in application-<profile>.yml
    connect-timeout: 10s
    read-timeout: 30s
    relaxed-https: false                      # true only for self-signed certificates in test environments
    report: true                              # requests and responses in the report
    headers:
      Accept: application/json
      Authorization: Bearer ${API_TOKEN}      # value from .env locally, CI secret in pipelines
```

Every key can be overridden like any other setting: `AUTTO_API_BASE_URL=...`, `-Dautto.api.read-timeout=60s`. See
the [configuration reference](configuration.md).

## A first API scenario

```gherkin
@api @nobrowser
Feature: Orders API

  Scenario: An order can be read back after it is created
    When the client creates an order for "Sauce Labs Backpack"
    Then the order is stored with status "PAID"
```

`@nobrowser` skips the browser start, so API scenarios take milliseconds.

```java
public class OrderApiSteps {

    private final Api api;
    private final ScenarioContext context;
    private final SoftAssertions softly;

    public OrderApiSteps(Api api, ScenarioContext context, SoftAssertions softly) {
        this.api = api;
        this.context = context;
        this.softly = softly;
    }

    @When("the client creates an order for {string}")
    public void theClientCreatesAnOrderFor(String product) {
        String id = api.request()
                .contentType(ContentType.JSON)
                .body(Map.of("product", product, "quantity", 1))      // POJOs and records work too
                .post("/orders")
                .then().statusCode(201)
                .extract().path("id");
        context.put("order.id", id);
    }

    @Then("the order is stored with status {string}")
    public void theOrderIsStoredWithStatus(String status) {
        Response order = api.request().get("/orders/{id}", context.get("order.id", String.class));
        softly.assertThat(order.statusCode()).isEqualTo(200);
        softly.assertThat(order.jsonPath().getString("status")).isEqualTo(status);
    }
}
```

The demo suite contains a working example: `features/api/storefront-api.feature`.

## Authentication

| Scheme | How |
|---|---|
| Static token / API key | `autto.api.headers.Authorization: Bearer ${API_TOKEN}` or `X-Api-Key: ${API_KEY}` |
| Per request | `api.request().auth().oauth2(token)`, `.auth().preemptive().basic(user, password)` |
| Token obtained at runtime | A bean that logs in once and caches the token, then `api.request().header("Authorization", "Bearer " + token)` |

Header values named like secrets (`Authorization`, `Cookie`, `X-Api-Key`, `*-Token`…) are always shown as `******`
in the report, and every value registered as a secret (see [Secrets](secrets.md)) is masked in bodies and logs.

## Report

Each request adds two collapsed blocks to the current step — *Request · POST /orders* and *Response · 201 · 85 ms* —
with headers and pretty-printed bodies (truncated after 20 000 characters), and one line to the log:

```
INFO  [Orders API - create] i.g.a.a.c.a.ApiReportFilter - POST https://api.qa.example.com/orders → 201 (85 ms)
```

Disable the report blocks with `autto.api.report=false` (high-volume data set-up, binary downloads).

## Preparing data for UI scenarios

Wrap the endpoints of your product in a bean and call it from `Given` steps:

```java
@Component
public class CartApi {
    private final Api api;
    public CartApi(Api api) { this.api = api; }

    public void addProducts(String sessionId, List<String> products) {
        products.forEach(p -> api.request().cookie("session-id", sessionId).body(Map.of("product", p))
                .post("/cart").then().statusCode(201));
    }
}
```

The browser then only checks what the user sees, which makes UI suites several times faster and less flaky.

## Going further

| Need | Add to `autto-e2e` |
|---|---|
| JSON Schema / contract validation | `io.rest-assured:json-schema-validator` → `then().body(matchesJsonSchemaInClasspath("schemas/order.json"))` |
| Mock external services | WireMock (`org.wiremock:wiremock-standalone`) as a Spring bean |
| GraphQL | Send the query as the JSON body: `body(Map.of("query", query, "variables", vars))` |
| Database checks | `spring-boot-starter-jdbc` + `JdbcClient` bean |
