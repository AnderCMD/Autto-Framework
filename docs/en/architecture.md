# Architecture

[← Back to README](../../README.md) · [Español](../es/arquitectura.md)

## Modules

```
autto-parent (pom)                    versions, plugins, quality gates (enforcer, checkstyle, jacoco)
├── autto-core   (jar, publishable)   the engine, packaged as a Spring Boot auto-configuration
└── autto-e2e    (tests only)         the suite of ONE application under test (demo: saucedemo.com)
```

`autto-core` knows nothing about any business; `autto-e2e` knows nothing about drivers, videos or reports. A company
publishes `autto-core` once and every product team creates its own `<product>-e2e` project that depends on it.

## Screaming architecture

> "Your architecture should tell readers about the system, not about the frameworks you used." — Robert C. Martin

Inside `autto-e2e` you see **what the product does**, not technical layers:

```
autto-e2e/src/test/java/io/github/andercmd/autto/e2e/
├── CucumberTestSuite.java             entry point (JUnit Platform suite)
├── CucumberSpringConfiguration.java   Cucumber ↔ Spring bridge
├── E2eTestApplication.java            Spring configuration of the suite (add your beans here)
├── shared/TestUsers.java              cross-feature support (users from application.yml + secrets)
└── features/
    ├── login/       LoginPage, LoginSteps
    ├── inventory/   InventoryPage, InventorySteps
    └── checkout/    CartPage, CheckoutPage, CheckoutSteps, Customer

autto-e2e/src/test/resources/
├── application.yml, application-<profile>.yml
├── features/<feature>/*.feature       same folder names as the code
└── testdata/<feature>/*.json
```

Everything of one feature lives together → high cohesion, clear ownership (`CODEOWNERS` per folder), easy deletion.
Reuse between features is explicit (constructor injection of another feature's page).

## Layers and dependency rule

```
features (Gherkin, steps, pages)  ──►  autto-core  ──►  Spring Boot · Cucumber · Selenium · WebDriverManager · Appium · Extent
```

### `autto-core` packages

| Package | Responsibility | Key classes |
|---|---|---|
| `config` | Spring Boot based configuration, profiles, `.env` | `AuttoProperties`, `AuttoSettings`, `DotEnv`, `DotEnvEnvironmentPostProcessor` |
| `spring` | Auto-configuration (beans for test projects) | `AuttoAutoConfiguration` |
| `driver` | Driver resolution and creation per thread | `DriverResolver` (WDM → Selenium Manager), `DriverFactory`, `DriverManager`, `DriverSession`, `BrowserOptionsFactory`, `CapabilitiesParser` |
| `ui` | Page object base and stereotype | `BasePage`, `@PageObject` |
| `cucumber` | Framework glue | `BrowserHooks` |
| `media` | Evidence capture | `Screenshots`, `VideoRecorder` |
| `report` | Extent report + public logging API | `ExtentCucumberPlugin`, `ExtentReportManager`, `Report`, `ReportPaths` |
| `security` | Secret masking | `Secrets`, `Credentials`, `MaskingMessageConverter` |
| `data` | JSON test data, random data | `TestData` |
| `context` | Per-scenario shared state | `ScenarioContext` |

## Dependency injection

| Bean | Scope | Provided by |
|---|---|---|
| `AuttoSettings`, `AuttoProperties` | singleton | `AuttoAutoConfiguration` |
| `ScenarioContext` | scenario | `AuttoAutoConfiguration` |
| `@PageObject` classes | scenario | component scan of `E2eTestApplication` |
| Step definitions, hooks | scenario | `cucumber-spring` (glue) |
| `TestUsers`, your API clients, builders… | singleton (or scenario) | component scan |

The Spring context is created once per run and shared by all scenarios and threads; scenario-scoped beans are
recreated for every scenario, so parallel execution is safe.

## Execution flow

```
./mvnw install
 └─ autto-e2e: Surefire → JUnit Platform → CucumberTestSuite (engine "cucumber")
     ├─ ExtentCucumberPlugin ← every Cucumber event (thread-safe), AuttoSettings loaded
     ├─ Spring context (once) ← application.yml + profile + .env + env vars + -D
     └─ for each scenario (optionally parallel)
         ├─ @Before BrowserHooks.startBrowser
         │     DriverFactory: local (WDM → Selenium Manager → Docker fallback) | docker | remote | appium
         │     VideoRecorder.start()
         ├─ steps (scenario-scoped beans) → @PageObject pages → DriverManager.driver()
         │     @AfterStep screenshot
         └─ @After evidence (URL, page source, console, video) → quit
 └─ TestRunFinished → Extent flush → autto-e2e/target/autto-reports/index.html
```

## Design decisions

See [Architecture decisions (ADR)](decisions.md) for Spring Boot, WebDriverManager, secrets and the multi-module
layout. Other choices:

| Decision | Reason |
|---|---|
| `By` constants instead of `@FindBy` | Stateless, parallel-safe page objects. |
| Driver looked up lazily (`DriverManager.driver()`) | Pages can be created before the browser exists; one browser per thread. |
| Implicit wait 0, explicit waits | Predictable timeouts (enforced by Checkstyle). |
| Custom Extent plugin | Full control, Cucumber 8 support, parallel, videos, masking. |
| Video from WebDriver screenshots + JCodec | Works headless, in Docker, Grid, cloud and Appium without ffmpeg. |
