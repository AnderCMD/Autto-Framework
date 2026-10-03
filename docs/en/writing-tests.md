# Writing tests

[← Back to README](../../README.md) · [Español](../es/escribir-pruebas.md)

This guide adds a business feature, **Product search**, to `autto-e2e`.

## 1. Folders

```
autto-e2e/src/test/resources/features/search/search.feature
autto-e2e/src/test/java/io/github/andercmd/autto/e2e/features/search/SearchPage.java
autto-e2e/src/test/java/io/github/andercmd/autto/e2e/features/search/SearchSteps.java
autto-e2e/src/test/resources/testdata/search/products.json        (optional)
```

> Feature files can be written in any Gherkin language (`# language: es` as first line). Cucumber binds steps by
> text, so the annotation language does not matter. Code stays in English.

## 2. Feature in business language

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

- Describe **behaviour**, not clicks. One scenario = one rule. Scenarios are independent.
- Tags: `@smoke`, `@regression`, `@critical`, feature tags, `@author:<name>`, `@nobrowser` (no browser),
  `@wip` / `@ignore` (excluded).
- Never write secrets in feature files; use user aliases (`"standard"`) resolved by `TestUsers`.

## 3. Page object

```java
@PageObject                                   // Spring bean, one instance per scenario
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

Rules (several are enforced by Checkstyle):

- `private static final By` locators, prefer `data-test` / `id`.
- Public methods express intent or return data; **no assertions**.
- Never store `WebElement` / `WebDriver`; call `driver()`.
- No `Thread.sleep`, no `implicitlyWait`, no `System.out`.
- Use `BasePage` helpers instead of raw Selenium:

| Need | Helpers |
|---|---|
| Navigation | `open`, `currentUrl`, `title`, `waitForPageLoad`, `waitForUrlContains` |
| Waits | `visible`, `clickable`, `allVisible`, `waitForInvisibility`, `waitForText`, `waitUntil` |
| Interactions | `click`, `jsClick` (last resort), `doubleClick`, `type`, `typeAndSubmit`, `pressKeys`, `selectByText`, `selectByValue`, `hover`, `scrollIntoView`, `upload` |
| Reading | `text`, `texts`, `attribute`, `isDisplayed`, `count` |
| Frames, windows, alerts | `switchToFrame`, `switchToDefaultContent`, `switchToNewWindow`, `switchToWindow`, `acceptAlert`, `dismissAlert` |
| Anything else | `js`, `driver()`, configuration via `config()` |

## 4. Step definitions

```java
public class SearchSteps {

    private final SearchPage search;

    public SearchSteps(SearchPage search) {          // constructor injection (Spring)
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

Steps can inject pages, `ScenarioContext`, `SoftAssertions`, `Api`, `TestUsers`, `AuttoProperties` or any bean of
your own.

### Soft assertions

Check several things and get **all** failures at once. The scenario-scoped `SoftAssertions` bean is verified
automatically when the scenario ends (before the failure evidence is collected):

```java
public CartSteps(CartPage cart, SoftAssertions softly) { ... }

@Then("the cart summary is correct")
public void theCartSummaryIsCorrect() {
    softly.assertThat(cart.count()).as("items").isEqualTo(2);
    softly.assertThat(cart.total()).as("total").isEqualTo("$39.98");
}
```

Use hard `assertThat` when the next steps make no sense after a failure (e.g. login).

## 5. Test users and secrets

```yaml
# application.yml
test-data:
  users:
    buyer:
      username: buyer@shop.test
      password: ${BUYER_PASSWORD}       # value in .env locally, CI secret in pipelines
```

```java
Credentials buyer = users.get("buyer");
loginPage.loginAs(buyer);               // toString() and reports show ******
```

See [Secrets & environment variables](secrets.md).

## 6. Shared state: `ScenarioContext`

```java
public CheckoutSteps(ScenarioContext context) { this.context = context; }
context.put("order.id", orderId);
String id = context.get("order.id", String.class);
```

## 7. Test data and random data

```java
List<Product> all = TestData.load("search/products.json", new TypeReference<List<Product>>() {});
Customer vip = TestData.entry("checkout/customers.yml", "vip", Customer.class);   // YAML works too
String email = TestData.faker().internet().emailAddress();
```

JSON and YAML values support `${NAME}` / `${NAME:default}` placeholders resolved from `.env`, environment and
configuration.

## 7b. Accessibility checks

```java
Accessibility.scan().assertNoViolations();                       // whole page, fails on autto.accessibility.fail-on
Accessibility.scan("#checkout-form").assertNoViolations(Impact.CRITICAL);   // one region, custom threshold
```

Every scan adds a table with the violated rules, impact and how to fix them to the report. The demo step
`Then the page has no critical accessibility violations` is reusable from any feature.

## 8. API calls and your own beans

The `Api` bean (REST Assured with the `autto.api.*` defaults, report and masking) covers API tests and data set-up
for UI scenarios — see [API testing](api-testing.md). Wrap the endpoints of your product in your own beans:

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

Database helpers, message clients or test-data builders are plain Spring beans too (add `spring-boot-starter-jdbc`
or the client you need to `autto-e2e`).

## 9. Run your feature

```bash
./mvnw -pl autto-e2e test -Dcucumber.filter.tags=@search
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/search
```
