# Escribir pruebas

[← Volver al README](../../README.es.md) · [English](../en/writing-tests.md)

Esta guía añade una funcionalidad de negocio, **Búsqueda de productos**, a `autto-e2e`.

## 1. Carpetas

```
autto-e2e/src/test/resources/features/search/search.feature
autto-e2e/src/test/java/io/github/andercmd/autto/e2e/features/search/SearchPage.java
autto-e2e/src/test/java/io/github/andercmd/autto/e2e/features/search/SearchSteps.java
autto-e2e/src/test/resources/testdata/search/products.json        (opcional)
```

> Los `.feature` pueden escribirse en cualquier idioma de Gherkin (`# language: es` en la primera línea, con
> `Característica`, `Escenario`, `Dado`, `Cuando`, `Entonces`). Cucumber enlaza los steps por texto, así que el
> idioma de la anotación no importa. El código se mantiene en inglés.

## 2. Funcionalidad en lenguaje de negocio

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

- Describe **comportamiento**, no clics. Un escenario = una regla. Escenarios independientes.
- Tags: `@smoke`, `@regression`, `@critical`, tags de funcionalidad, `@author:<nombre>`, `@nobrowser` (sin
  navegador), `@wip` / `@ignore` (excluidos).
- Nunca escribas secretos en los `.feature`; usa alias de usuario (`"standard"`) que resuelve `TestUsers`.

## 3. Page object

```java
@PageObject                                   // bean de Spring, una instancia por escenario
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

Reglas (varias las verifica Checkstyle):

- Locators `private static final By`; prefiere `data-test` / `id`.
- Los métodos públicos expresan intención o devuelven datos; **sin aserciones**.
- Nunca guardes `WebElement` / `WebDriver`; llama a `driver()`.
- Nada de `Thread.sleep`, `implicitlyWait` ni `System.out`.
- Usa los helpers de `BasePage`: `open`, `click`, `type`, `text`, `texts`, `visible`, `clickable`, `allVisible`,
  `selectByText`, `hover`, `isDisplayed`, `waitUntil`, `js`; la configuración con `config()`.

## 4. Step definitions

```java
public class SearchSteps {

    private final SearchPage search;

    public SearchSteps(SearchPage search) {          // inyección por constructor (Spring)
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

Los steps pueden inyectar páginas, `ScenarioContext`, `TestUsers`, `AuttoProperties` o cualquier bean propio.

## 5. Usuarios de prueba y secretos

```yaml
# application.yml
test-data:
  users:
    buyer:
      username: buyer@shop.test
      password: ${BUYER_PASSWORD}       # valor en .env en local, secreto del CI en pipelines
```

```java
Credentials buyer = users.get("buyer");
loginPage.loginAs(buyer);               // toString() y los reportes muestran ******
```

Ver [Secretos y variables de entorno](secretos.md).

## 6. Estado compartido: `ScenarioContext`

```java
public CheckoutSteps(ScenarioContext context) { this.context = context; }
context.put("order.id", orderId);
String id = context.get("order.id", String.class);
```

## 7. Datos de prueba y datos aleatorios

```java
List<Product> all = TestData.load("search/products.json", new TypeReference<List<Product>>() {});
String email = TestData.faker().internet().emailAddress();
```

Los valores JSON admiten placeholders `${NOMBRE}` / `${NOMBRE:defecto}` resueltos desde `.env`, el entorno y la
configuración.

## 8. Tus propios beans (clientes de API, utilidades de BD…)

```java
@Component
public class OrdersApi {
    private final RestClient client;
    public OrdersApi(AuttoProperties props) {
        this.client = RestClient.create(props.baseUrl() + "/api");   // añade spring-web a autto-e2e
    }
}
```

Úsalos en los steps para crear datos rápidamente o verificar el estado del back-end.

## 9. Ejecuta tu funcionalidad

```bash
./mvnw -pl autto-e2e test -Dcucumber.filter.tags=@search
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/search
```
