# Architecture

[← Back to README](../../README.md) · [Español](../es/arquitectura.md)

## Screaming architecture

> "Your architecture should tell readers about the system, not about the frameworks you used." — Robert C. Martin

When you open `src/test/java/.../features` you see **what the product does** (`login`, `inventory`, `checkout`), not
technical layers such as `pages/`, `steps/`, `models/`. Everything needed for one business feature lives together:

```
features/checkout/
├── CartPage.java          page object(s) of the feature
├── CheckoutPage.java
├── CheckoutSteps.java     step definitions of the feature
└── Customer.java          models / builders used by the feature

resources/features/checkout/checkout.feature    Gherkin, same folder name
resources/testdata/checkout/...                 data, same folder name
```

Benefits:

- **Cohesion**: changing a feature touches one folder.
- **Ownership**: squads can own folders (`CODEOWNERS`).
- **Scalability**: hundreds of features do not turn into a single `pages/` package with hundreds of classes.
- **Deletion is easy**: removing a feature means removing a folder.

Cross-feature reuse is explicit: `CheckoutSteps` injects `InventoryPage` from `features.inventory`. Generic,
feature-agnostic code lives in `shared/` (test side) or in the `core` framework (main side).

## Layers

```
┌──────────────────────────────────────────────────────────────────────┐
│ src/test  ── WHAT is tested                                          │
│   features/<feature>/*.feature   business language (Gherkin)         │
│   features/<feature>/*Steps      glue: Gherkin → page objects        │
│   features/<feature>/*Page       UI knowledge: locators + actions    │
│   shared/hooks                   browser life cycle + evidence       │
├──────────────────────────────────────────────────────────────────────┤
│ src/main  ── HOW (framework core, reusable, no business knowledge)   │
│   config  driver  ui  media  report  data  context                   │
├──────────────────────────────────────────────────────────────────────┤
│ Libraries: Cucumber · JUnit Platform · Selenium · Appium · Extent    │
└──────────────────────────────────────────────────────────────────────┘
```

Dependency rule: `features` → `core` → libraries. The core never depends on test code.

### Core packages

| Package | Responsibility | Key classes |
|---|---|---|
| `core.config` | Layered configuration with typed getters | `AuttoConfig`, `ConfigKeys` |
| `core.driver` | Creates and owns browsers/devices per thread | `DriverFactory`, `BrowserOptionsFactory`, `DriverManager`, `DriverSession`, `CapabilitiesParser` |
| `core.ui` | Base page object with explicit waits and safe actions | `BasePage` |
| `core.media` | Evidence capture | `Screenshots`, `VideoRecorder`, `EvidenceMode` |
| `core.report` | Extent report generation and public logging API | `ExtentCucumberPlugin`, `ExtentReportManager`, `Report`, `ReportPaths` |
| `core.data` | JSON test data with `${placeholders}` and random data | `TestData` |
| `core.context` | Per-scenario state shared between step classes | `ScenarioContext` |

## Execution flow

```
mvn test
 └─ Surefire → JUnit Platform → CucumberTestSuite (@Suite, engine "cucumber")
     ├─ ExtentCucumberPlugin  ← receives every Cucumber event (thread-safe)
     └─ for each scenario (optionally in parallel threads)
         ├─ @Before  BrowserHooks.startBrowser
         │     DriverManager.start() → DriverFactory → local | remote | appium
         │     VideoRecorder.start()  (if video.mode != off)
         ├─ steps → page objects → DriverManager.driver()
         │     @AfterStep screenshot (screenshot.mode)
         └─ @After BrowserHooks.collectEvidenceAndQuit
               URL, page source, console logs, video → scenario.attach(...)
               DriverManager.quit()
 └─ TestRunFinished → Extent flush → target/autto-reports/index.html
```

## Design decisions

| Decision | Reason |
|---|---|
| `By` constants instead of `@FindBy` / `PageFactory` | Page objects are stateless and parallel-safe; no stale proxies. |
| Pages look up the driver lazily (`DriverManager.driver()`) | PicoContainer can build pages before the browser exists; one browser per thread. |
| Implicit wait = 0, explicit waits everywhere | Mixing both causes unpredictable timeouts. |
| Custom Extent plugin instead of the third-party adapter | Full control of the report, works with Cucumber 8, supports parallel runs, videos and hook evidence. |
| Video from WebDriver screenshots + JCodec | Works everywhere (headless, Grid, cloud, Appium) without ffmpeg or a desktop session. |
| Properties + env vars + `-D` | Zero-code switching between local, CI and cloud; secrets never committed. |
| PicoContainer | Lightweight constructor injection; a fresh object graph per scenario. |
