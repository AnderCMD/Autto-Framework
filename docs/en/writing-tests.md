# Writing tests

[← Back to README](../../README.md) · [Español](../es/escribir-pruebas.md)

This guide adds a new business feature, **Product search**, from scratch.

## 1. Create the folders

```
src/test/resources/features/search/search.feature
src/test/java/io/github/andercmd/autto/features/search/SearchPage.java
src/test/java/io/github/andercmd/autto/features/search/SearchSteps.java
src/test/resources/testdata/search/products.json        (optional)
```

> Feature files can be written in any Gherkin language by adding `# language: es` (or `fr`, `pt`…) as the first
> line. Cucumber binds steps by text, so the annotation language (`@Given` or `@Dado`) does not matter.

## 2. Write the feature in business language

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

Guidelines:

- Describe **behaviour**, not clicks (`the customer searches for`, not `I click the search box`).
- One scenario = one rule. Keep them independent: never rely on another scenario's state.
- Tags: `@smoke`, `@regression`, `@critical`, feature tags (`@search`), `@author:<name>` (shown in the report),
  `@nobrowser` (no browser is started), `@wip` / `@ignore` (excluded by default).

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

Rules for page objects:

- Locators are `private static final By` constants. Prefer `data-test` / `id` attributes over CSS classes or XPath.
- Public methods express user intent (`searchFor`), return data (`resultNames`) or other pages; **no assertions**.
- Never store `WebElement` or `WebDriver` in fields; call `driver()` (one browser per thread).
- Use the `BasePage` helpers: `open`, `click`, `type`, `text`, `texts`, `visible`, `clickable`, `allVisible`,
  `selectByText`, `hover`, `isDisplayed`, `waitUntil`, `js`…

## 4. Step definitions

```java
package io.github.andercmd.autto.features.search;

public class SearchSteps {

    private final SearchPage search;

    public SearchSteps(SearchPage search) {     // injected by PicoContainer
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

- Step classes are created per scenario; declare dependencies (pages, `ScenarioContext`, other helpers) in the
  constructor.
- Assertions belong here (AssertJ). Use `.as("description")` for readable failures.
- Reuse steps across features freely (e.g. `the customer is logged in as {string}` lives in `features/login`).

## 5. Sharing data between steps: `ScenarioContext`

```java
public CheckoutSteps(ScenarioContext context) { this.context = context; }

context.put("order.id", orderId);
String id = context.get("order.id", String.class);
```

A new context is created for each scenario, so nothing leaks between scenarios or threads.

## 6. Test data

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

`${key:default}` placeholders are resolved from the configuration, so secrets come from
`AUTTO_USERS_PASSWORD` (env var) or `-Dusers.password=...`, never from the repository.

## 7. Enrich the report (optional)

```java
Report.info("Searching " + text);
Report.screenshot("Search results");
Report.table("Filters", Map.of("category", "bags", "sort", "price"));
Report.json("API response", body);
```

See [Reports & evidence](reporting.md).

## 8. Run only your feature

```bash
./mvnw test -Dcucumber.filter.tags=@search
./mvnw test -Dcucumber.features=classpath:features/search
```

## Scenarios without a browser

Tag API, database or pure-logic scenarios with `@nobrowser` so the hooks do not start a browser. The report and
`Report` API still work.
