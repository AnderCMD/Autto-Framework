# Pruebas de API

[← Volver al README](../../README.es.md) · [English](../en/api-testing.md)

Autto incluye [REST Assured](https://rest-assured.io) detrás del bean `Api`. Úsalo para probar servicios
directamente, para preparar datos de los escenarios de UI en milisegundos en vez de rellenar formularios y para
verificar el estado del back-end.

## Configuración

```yaml
autto:
  api:
    base-url: https://api.qa.example.com      # por entorno en application-<perfil>.yml
    connect-timeout: 10s
    read-timeout: 30s
    relaxed-https: false                      # true solo para certificados autofirmados en entornos de prueba
    report: true                              # peticiones y respuestas en el reporte
    headers:
      Accept: application/json
      Authorization: Bearer ${API_TOKEN}      # valor en .env en local, secreto del CI en los pipelines
```

Cada clave se puede sobrescribir como cualquier otro ajuste: `AUTTO_API_BASE_URL=...`, `-Dautto.api.read-timeout=60s`.
Ver la [referencia de configuración](configuracion.md).

## Un primer escenario de API

```gherkin
@api @nobrowser
Feature: Orders API

  Scenario: An order can be read back after it is created
    When the client creates an order for "Sauce Labs Backpack"
    Then the order is stored with status "PAID"
```

`@nobrowser` evita arrancar el navegador, así que los escenarios de API tardan milisegundos.

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
                .body(Map.of("product", product, "quantity", 1))      // también POJOs y records
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

La suite de demo contiene un ejemplo funcional: `features/api/storefront-api.feature`.

## Autenticación

| Esquema | Cómo |
|---|---|
| Token fijo / API key | `autto.api.headers.Authorization: Bearer ${API_TOKEN}` o `X-Api-Key: ${API_KEY}` |
| Por petición | `api.request().auth().oauth2(token)`, `.auth().preemptive().basic(user, password)` |
| Token obtenido en ejecución | Un bean que hace login una vez y guarda el token, luego `api.request().header("Authorization", "Bearer " + token)` |

Los valores de cabeceras con nombre de secreto (`Authorization`, `Cookie`, `X-Api-Key`, `*-Token`…) se muestran
siempre como `******` en el reporte, y cualquier valor registrado como secreto (ver [Secretos](secretos.md)) se
enmascara en cuerpos y logs.

## Reporte

Cada petición añade dos bloques plegados al step actual — *Request · POST /orders* y *Response · 201 · 85 ms* — con
cabeceras y cuerpos formateados (truncados a partir de 20 000 caracteres), y una línea al log:

```
INFO  [Orders API - create] i.g.a.a.c.a.ApiReportFilter - POST https://api.qa.example.com/orders → 201 (85 ms)
```

Desactiva los bloques del reporte con `autto.api.report=false` (preparación masiva de datos, descargas binarias).

## Preparar datos para escenarios de UI

Envuelve los endpoints de tu producto en un bean y llámalo desde los steps `Given`:

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

El navegador solo comprueba lo que ve el usuario, lo que hace las suites de UI varias veces más rápidas y estables.

## Más allá

| Necesidad | Añade a `autto-e2e` |
|---|---|
| Validación de JSON Schema / contratos | `io.rest-assured:json-schema-validator` → `then().body(matchesJsonSchemaInClasspath("schemas/order.json"))` |
| Simular servicios externos | WireMock (`org.wiremock:wiremock-standalone`) como bean de Spring |
| GraphQL | Envía la consulta como cuerpo JSON: `body(Map.of("query", query, "variables", vars))` |
| Comprobaciones en base de datos | `spring-boot-starter-jdbc` + un bean `JdbcClient` |
