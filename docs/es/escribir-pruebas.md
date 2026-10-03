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
- Usa los helpers de `BasePage` en lugar de Selenium directo:

| Necesidad | Helpers |
|---|---|
| Navegación | `open`, `currentUrl`, `title`, `waitForPageLoad`, `waitForUrlContains` |
| Esperas | `visible`, `clickable`, `allVisible`, `waitForInvisibility`, `waitForText`, `waitUntil` |
| Interacciones | `click`, `jsClick` (último recurso), `doubleClick`, `type`, `typeAndSubmit`, `pressKeys`, `selectByText`, `selectByValue`, `hover`, `scrollIntoView`, `upload` |
| Lectura | `text`, `texts`, `attribute`, `isDisplayed`, `count` |
| Frames, ventanas, alertas | `switchToFrame`, `switchToDefaultContent`, `switchToNewWindow`, `switchToWindow`, `acceptAlert`, `dismissAlert` |
| Cualquier otra cosa | `js`, `driver()`, la configuración con `config()` |

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

### Aserciones suaves (soft assertions)

Comprueba varias cosas y obtén **todos** los fallos a la vez. El bean `SoftAssertions` (uno por escenario) se verifica
automáticamente al terminar el escenario (antes de recoger las evidencias del fallo):

```java
public CartSteps(CartPage cart, SoftAssertions softly) { ... }

@Then("the cart summary is correct")
public void theCartSummaryIsCorrect() {
    softly.assertThat(cart.count()).as("items").isEqualTo(2);
    softly.assertThat(cart.total()).as("total").isEqualTo("$39.98");
}
```

Usa `assertThat` normal cuando los siguientes pasos no tienen sentido tras un fallo (p. ej. el login).

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
Customer vip = TestData.entry("checkout/customers.yml", "vip", Customer.class);   // también YAML
String email = TestData.faker().internet().emailAddress();
```

Los valores JSON y YAML admiten placeholders `${NOMBRE}` / `${NOMBRE:defecto}` resueltos desde `.env`, el entorno y la
configuración.

## 7b. Comprobaciones de accesibilidad

```java
Accessibility.scan().assertNoViolations();                       // página completa, falla según autto.accessibility.fail-on
Accessibility.scan("#checkout-form").assertNoViolations(Impact.CRITICAL);   // una región, umbral propio
```

Cada auditoría añade al reporte una tabla con las reglas incumplidas, su impacto y cómo corregirlas. El step de la
demo `Then the page has no critical accessibility violations` se puede reutilizar desde cualquier funcionalidad.

## 8. Llamadas a APIs y tus propios beans

El bean `Api` (REST Assured con los valores de `autto.api.*`, reporte y enmascarado) cubre las pruebas de API y la
preparación de datos para escenarios de UI — ver [Pruebas de API](pruebas-api.md). Envuelve los endpoints de tu
producto en tus propios beans:

```java
@Component
public class OrdersApi {
    private final Api api;
    public OrdersApi(Api api) { this.api = api; }

    public String createOrder(Order order) {
        return api.request().body(order).post("/orders").then().statusCode(201).extract().path("id");
    }
}
```

Las utilidades de base de datos, clientes de mensajería o builders de datos también son beans de Spring normales
(añade `spring-boot-starter-jdbc` o el cliente que necesites a `autto-e2e`).

## 9. Ejecuta tu funcionalidad

```bash
./mvnw -pl autto-e2e test -Dcucumber.filter.tags=@search
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/search
```
