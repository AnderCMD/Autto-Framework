# Escribir pruebas

[← Volver al README](../../README.es.md) · [English](../en/writing-tests.md)

Esta guía añade una nueva funcionalidad de negocio, **Búsqueda de productos**, desde cero.

## 1. Crea las carpetas

```
src/test/resources/features/search/search.feature
src/test/java/io/github/andercmd/autto/features/search/SearchPage.java
src/test/java/io/github/andercmd/autto/features/search/SearchSteps.java
src/test/resources/testdata/search/products.json        (opcional)
```

> El código (clases, métodos, variables, comentarios) va en inglés. Los `.feature` pueden escribirse en español
> añadiendo `# language: es` en la primera línea y usando `Característica`, `Escenario`, `Dado`, `Cuando`,
> `Entonces`. Cucumber enlaza los steps por texto, así que cualquier anotación (`@Given` o `@Dado`) funciona.

## 2. Escribe la funcionalidad en lenguaje de negocio

```gherkin
@search @regression
Feature: Product search
  As a customer
  I want to search the catalog
  So that I find products quickly

  Background:
    Given the customer is logged in as "standard"

  @smoke @author:jane.doe
  Scenario: Search by name
    When the customer searches for "backpack"
    Then only products containing "backpack" are listed
```

Buenas prácticas:

- Describe **comportamiento**, no clics (`the customer searches for`, no `I click the search box`).
- Un escenario = una regla. Mantenlos independientes: nunca dependas del estado de otro escenario.
- Tags: `@smoke`, `@regression`, `@critical`, tags de funcionalidad (`@search`), `@author:<nombre>` (se muestra en el
  reporte), `@nobrowser` (no se abre navegador), `@wip` / `@ignore` (excluidos por defecto).

## 3. Page object

```java
package io.github.andercmd.autto.features.search;

public class SearchPage extends BasePage {

    private static final By SEARCH_BOX = By.cssSelector("[data-test='search']");
    private static final By RESULT_NAME = By.cssSelector("[data-test='result-name']");

    public void searchFor(String text) {
        type(SEARCH_BOX, text);
        driver().findElement(SEARCH_BOX).submit();
    }

    public List<String> resultNames() {
        allVisible(RESULT_NAME);
        return texts(RESULT_NAME);
    }
}
```

Reglas para page objects:

- Los locators son constantes `private static final By`. Prefiere atributos `data-test` / `id` antes que clases CSS o
  XPath.
- Los métodos públicos expresan intención del usuario (`searchFor`), devuelven datos (`resultNames`) u otras páginas;
  **sin aserciones**.
- Nunca guardes `WebElement` ni `WebDriver` en atributos; llama a `driver()` (un navegador por hilo).
- Usa los helpers de `BasePage`: `open`, `click`, `type`, `text`, `texts`, `visible`, `clickable`, `allVisible`,
  `selectByText`, `hover`, `isDisplayed`, `waitUntil`, `js`…

## 4. Step definitions

```java
package io.github.andercmd.autto.features.search;

public class SearchSteps {

    private final SearchPage search;

    public SearchSteps(SearchPage search) {     // inyectado por PicoContainer
        this.search = search;
    }

    @When("the customer searches for {string}")
    public void theCustomerSearchesFor(String text) {
        search.searchFor(text);
    }

    @Then("only products containing {string} are listed")
    public void onlyProductsContaining(String text) {
        List<String> names = search.resultNames();
        Report.info("Results: " + names);
        assertThat(names).isNotEmpty().allMatch(n -> n.toLowerCase().contains(text));
    }
}
```

- Las clases de steps se crean por escenario; declara sus dependencias (páginas, `ScenarioContext`, helpers) en el
  constructor.
- Las aserciones van aquí (AssertJ). Usa `.as("descripción")` para fallos legibles.
- Reutiliza steps entre funcionalidades (por ejemplo `the customer is logged in as {string}` vive en `features/login`).

## 5. Compartir datos entre steps: `ScenarioContext`

```java
public CheckoutSteps(ScenarioContext context) { this.context = context; }

context.put("order.id", orderId);
String id = context.get("order.id", String.class);
```

Se crea un contexto nuevo por escenario, así que nada se filtra entre escenarios ni hilos.

## 6. Datos de prueba

`src/test/resources/testdata/login/users.json`:

```json
{
  "standard": { "username": "standard_user", "password": "${users.password:secret_sauce}" }
}
```

```java
Credentials user = TestData.entry("login/users.json", "standard", Credentials.class);
List<Product> all = TestData.load("search/products.json", new TypeReference<List<Product>>() {});
String email = TestData.faker().internet().emailAddress();
```

Los placeholders `${clave:valor_por_defecto}` se resuelven desde la configuración, así los secretos llegan por
`AUTTO_USERS_PASSWORD` (variable de entorno) o `-Dusers.password=...`, nunca desde el repositorio.

## 7. Enriquecer el reporte (opcional)

```java
Report.info("Searching " + text);
Report.screenshot("Search results");
Report.table("Filters", Map.of("category", "bags", "sort", "price"));
Report.json("API response", body);
```

Ver [Reportes y evidencias](reportes.md).

## 8. Ejecuta solo tu funcionalidad

```bash
./mvnw test -Dcucumber.filter.tags=@search
./mvnw test -Dcucumber.features=classpath:features/search
```

## Escenarios sin navegador

Etiqueta los escenarios de API, base de datos o lógica pura con `@nobrowser` para que los hooks no abran un
navegador. El reporte y la API `Report` siguen funcionando.
