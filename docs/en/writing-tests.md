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
- Use `BasePage` helpers: `open`, `click`, `type`, `text`, `texts`, `visible`, `clickable`, `allVisible`,
  `selectByText`, `hover`, `isDisplayed`, `waitUntil`, `js`; configuration via `config()`.

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

Steps can inject pages, `ScenarioContext`, `TestUsers`, `AuttoProperties` or any bean of your own.

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
String email = TestData.faker().internet().emailAddress();
```

JSON values support `${NAME}` / `${NAME:default}` placeholders resolved from `.env`, environment and configuration.

## 8. Your own beans (API clients, DB helpers…)

```java
@Component
public class OrdersApi {
    private final RestClient client;
    public OrdersApi(AuttoProperties props) {
        this.client = RestClient.create(props.baseUrl() + "/api");   // add spring-web to autto-e2e
    }
}
```

Use them in steps to create test data quickly or to verify back-end state.

## 9. Run your feature

```bash
./mvnw -pl autto-e2e test -Dcucumber.filter.tags=@search
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/search
```
