# Advanced testing and robustness

[← Back to README](../../README.md) · [Español](../es/pruebas-avanzadas.md)

Everything here is optional and off the critical path: use only what your suite needs. All classes live in
`autto-core`; every setting has a safe default and is documented in [Configuration](configuration.md).

## Make runs stable

### Stale elements are retried for you

Single-page applications re-render constantly, so `StaleElementReferenceException` is the most common cause of flaky
UI tests. `BasePage` retries the whole interaction (locate → act) up to 3 times in `type`, `text`, `texts`,
`attribute`, `select*`, `jsClick`, `doubleClick`, `pressKeys` and `hover`; `click` and the waits already did. Wrap
your own interactions with `retryStale(() -> ...)`.

### Scenario timeout

`autto.scenario.timeout` (default `10m`, `0` disables) aborts a scenario that hangs: its browser is closed, the worker
thread is interrupted and the scenario fails with a message in the report. A hung scenario can no longer block a
parallel run.

### Rerun failed scenarios and flag flaky ones

```bash
./scripts/run-with-rerun.sh -Dcucumber.filter.tags=@smoke -Dautto.browser.headless=true
```

Pass 1 runs normally. If scenarios fail, **only those** run once more into
`autto-e2e/target/autto-reports-rerun` (scenarios get the `rerun` category). A scenario that passes the second time
is *flaky*: it does not fail the build, but it is counted in `metrics.json` (`recovered_on_rerun`) and visible in the
rerun report so it can be fixed. A scenario failing twice fails the build. The CI workflows use this script.

### Unique test data

Parallel scenarios against a shared environment collide on e-mails, user names and references. `Unique` generates
values that never repeat across threads, scenarios and runs:

```java
String email = Unique.email();          // autto.k3j9x2.1@example.test
String user  = Unique.of("customer");   // customer-k3j9x2-2
```

Use `TestData.faker()` (Datafaker) for realistic names and addresses, `Unique` for anything that must be unique.

### Driver and browser out of sync

Browsers auto-update; a cached driver then refuses to start (`This version of ChromeDriver only supports Chrome version
150`). Autto detects it, forgets the cached resolution, resolves the driver again and retries once, so a Chrome update
overnight does not break the first run of the morning.

## Environment tags

Tags change the browser of one scenario without code:

| Tag | Effect |
|---|---|
| `@viewport:390x844` | Resizes the window (phone, tablet, desktop layouts) |
| `@slow-network` | Throttles the network, 400 kbps and 400 ms latency (Chromium) |
| `@offline` | Cuts the network (Chromium) |
| `@nobrowser` | No browser at all (API, data and logic scenarios) |

For other languages or locales set `autto.browser.prefs.intl.accept_languages` per profile (`application-es.yml`).

## Data and services

### Database

`Database` is a scenario-scoped JDBC helper: add your database driver to the test project and set `autto.db.url`,
`autto.db.username`, `autto.db.password` (masked in logs and reports).

```java
public OrderSteps(Database db) { this.db = db; }

db.update("insert into users(email, status) values (?, ?)", email, "NEW");
db.cleanup("delete from users where email = ?", email);       // runs when the scenario ends, even if it failed
assertThat(db.scalar("select status from orders where ref = ?", ref)).contains("PAID");
```

Always bind values with `?`; never concatenate them into the SQL.

### E-mail and one-time codes

```bash
docker compose --profile mail up -d       # Mailpit: SMTP localhost:1025, UI and API http://localhost:8025
```

Point the application under test to the SMTP port and read what it sent:

```java
Mailbox.Mail mail = mailbox.waitFor(email, "Reset your password");   // polls up to autto.mail.timeout
String code = mail.find("code is (\\d{6})").orElseThrow();
```

For authenticator-app two-factor authentication keep the Base32 secret of the test account in `.env` and use
`Totp.now(secret)`.

### Contract testing

Keep JSON Schema files in `src/test/resources/contracts/` (export them from your OpenAPI document) and verify the
responses:

```java
Response response = api.request().get("/orders/42");
ApiContract.assertMatches(response, "order.json");
```

Every API request also carries an `X-Correlation-Id` header, the id printed in every log line of the scenario
(`%X{correlationId}`), so a failing scenario can be traced through the logs of the application under test.

## Quality of the page

### Web performance budgets

```java
WebPerformance.measure().assertWithinBudget();
```

Reads Navigation and Paint Timing (TTFB, FCP, LCP, CLS, load) of the current page, adds a table to the report and fails
listing **every** metric over its budget (`autto.performance.*`; defaults are the Core Web Vitals "good" thresholds,
relax them per environment). LCP and CLS are Chromium-only; budgets of metrics the browser does not report are
skipped.

### Visual regression

```java
VisualRegression.assertMatches("login-page");
VisualRegression.assertMatches("inventory", new Rectangle(0, 0, 1920, 120));   // ignored area
```

1. First run: `-Dautto.visual.update=true` writes the baseline to `autto.visual.baseline-dir`; review and commit it.
2. Next runs compare pixels. The page is first *settled* (web fonts loaded, two identical consecutive screenshots) so
   animations and font swaps do not create false differences.
3. A failure writes `<name>-actual.png` and a red `<name>-diff.png` next to the report and embeds the diff in it.

Differences below `autto.visual.pixel-threshold` per channel and up to `autto.visual.tolerance` of different pixels
are accepted. Baselines depend on browser, version, OS and fonts: create and compare them in the same environment
(the Docker target is the most reproducible).

### Network mocking

```java
network.stub("/api/cart", 500, "{\"error\":\"boom\"}");    // the UI must show an error banner
network.block("googletagmanager.com");                      // third parties never slow a test down
network.delay("/api/products", Duration.ofSeconds(3));      // loading indicators
```

Chrome and Edge use the DevTools protocol (Selenium's `NetworkInterceptor`, which needs the DevTools bindings that match
the browser version: Selenium ships the latest ones). Firefox, and Chromium versions without matching bindings, fall
back to WebDriver BiDi (`autto.browser.console-logs: true`, the default). `delay` forwards the real request after the
pause. Verified on Chrome 154: stubbed responses, blocked hosts and delayed requests (see `features/quality`). WebDriver
BiDi request interception on Chrome 150 failed with *Invalid InterceptionId*, hence the DevTools engine first.

### Accessibility

See [Writing tests](writing-tests.md#accessibility) (`Accessibility.scan()`).

## Observe the run

| Output | Where | Use |
|---|---|---|
| `metrics.json` | next to the report | Scenarios, passed, failed, flaky, browser start retries, durations |
| `metrics.prom` | next to the report | Prometheus text format (node_exporter *textfile collector*) |
| Notification | Slack, Microsoft Teams or a generic JSON webhook | Summary with link to the run, optionally only on failure |
| JSON logs | `-Dlogback.configurationFile=autto/logback-json.xml` | One JSON object per line with scenario and correlation id for Datadog, ELK, Loki |

```yaml
autto:
  notifications:
    webhook-url: ${SLACK_WEBHOOK_URL}   # a secret: .env locally, CI secret in pipelines
    type: slack                          # slack | teams | generic
    only-on-failure: true
    report-url: ${REPORT_URL:}
```

Register the plugin once in `junit-platform.properties`:
`io.github.andercmd.autto.core.observability.RunSummaryPlugin`.

### Allure and test-management tools

Add `io.github.andercmd.autto.core.report.AllureResultsPlugin` to `cucumber.plugin` and it writes Allure result files
(`target/allure-results`, `-Dautto.allure.results=...` to change it) with steps, tags as labels, failure messages and
traces, and every attachment (screenshots, video, page source). View them with `allure serve target/allure-results`.
This is Autto's own writer because the official `allure-cucumber7-jvm` plugin does not work with the Cucumber 8
message API used here (`NoSuchMethodError`).

Xray, Zephyr: import `target/autto-reports/cucumber/cucumber.json`. TestRail, Azure DevOps: `cucumber-junit.xml`.

## Native mobile apps

Same features, hooks, evidence and report through Appium: use the `appium` profile
(`application-appium.yml`: Android emulator, system Settings app) and the demo `@mobile` feature, then replace the
capabilities with your app. Screen objects extend `BasePage` and use `AppiumBy` locators.

## Check an environment

```bash
./scripts/doctor.sh
```

Verifies the JDK, the configuration, `.env`, reachability of `autto.base-url`, the report folder, the installed
browsers and Docker, and tells what is missing. Run it first on a new machine or CI runner.

## Start a new project

```bash
./scripts/new-project.sh ../checkout-e2e com.acme checkout-e2e           # engine from JitPack
./scripts/new-project.sh ../checkout-e2e com.acme checkout-e2e --local   # engine from ./mvnw install
```

Creates a standalone project (pom, suite, Spring configuration, a first feature, logging, `.env.example`, Maven
wrapper) that builds and passes against a real browser out of the box. The repository also ships a
`.devcontainer/` (JDK 21, Chromium, Docker access) so a contributor can start with one click in VS Code or Codespaces.

## Supply-chain quality gates

| Command | What it does |
|---|---|
| `./mvnw -Pcoverage -pl autto-core verify` | JaCoCo report and a minimum line coverage (`coverage.minimum`, 50 % today; raise it as coverage grows) |
| `./mvnw -Pquality verify -DskipTests` | SpotBugs (null dereferences, unclosed resources...), JDK 21 or 25 |
| `./mvnw -Psbom package` | CycloneDX SBOM at `target/bom.json` |
| `./mvnw -Prelease deploy` | Javadoc, GPG signatures and publication to Maven Central |

CI pins every GitHub Action to a commit SHA (Dependabot keeps them current), scans dependencies with OSV every night
and with the dependency-review action on pull requests. Pushing a tag `vX.Y.Z` runs the **Release** workflow: build,
tests, SBOM, build-provenance attestation, keyless Sigstore signatures, GitHub release and, when the
`MAVEN_CENTRAL_*` / `GPG_*` secrets exist, Maven Central.

Verify a downloaded release:

```bash
gh attestation verify autto-core-1.2.0.jar --repo AnderCMD/Autto-Framework
cosign verify-blob --bundle autto-core-1.2.0.jar.sigstore.json \
  --certificate-identity-regexp 'https://github.com/AnderCMD/Autto-Framework/.*' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com autto-core-1.2.0.jar
```
