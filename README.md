<div align="center">

# Autto Framework

**Open source QA automation template · Cucumber · Selenium · Extent Reports · Java 27**

[![CI](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml/badge.svg)](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-27-orange)
![Cucumber](https://img.shields.io/badge/Cucumber-8.0.4-23d96c)
![Selenium](https://img.shields.io/badge/Selenium-4.50.0-43b02a)

English · [Español](README.es.md)

</div>

---

Autto is a ready-to-use, production-grade starting point for UI test automation. Clone it, rename the package and
start writing scenarios: browsers, drivers, parallel execution, evidence and beautiful reports are already solved.

## Highlights

| | |
|---|---|
| **BDD** | Cucumber 8 on the JUnit Platform 6 engine, PicoContainer dependency injection, parallel scenarios. |
| **Screaming architecture** | Code is organized by business feature (`features/login`, `features/checkout`…), not by technical layer. |
| **Any browser** | Chrome, Firefox, Edge and Safari. Drivers are resolved automatically by Selenium Manager (it can even download Chrome/Firefox when missing). |
| **Any platform** | Windows, macOS, Linux, Docker, Selenium Grid, BrowserStack / Sauce Labs / LambdaTest, Android and iOS (Appium). |
| **Rich reports** | Extent Spark dashboard with timeline, tags, devices, authors, failures-only report and JSON archive. |
| **Evidence** | Screenshots, **MP4 video of every scenario** (no ffmpeg needed), page source, browser console logs (BiDi), attachments. |
| **Configuration** | One properties file + per-environment files, overridable with `AUTTO_*` env vars or `-D` flags. |
| **CI ready** | GitHub Actions matrix (Windows/macOS/Linux × Chrome/Firefox/Edge + Safari), nightly Grid regression, Dependabot, Maven Wrapper. |

## Tech stack

| Component | Version |
|---|---|
| Java | 27 (configurable through `java.version`) |
| Cucumber JVM | 8.0.4 |
| Selenium | 4.50.0 |
| Appium Java client | 10.1.1 |
| JUnit Platform | 6.1.3 |
| Extent Reports | 5.1.2 |
| AssertJ · Datafaker · Jackson · Awaitility · Logback | latest stable |
| Maven (wrapper) | 3.9.16 |

> **About Java 27.** The project targets Java 27, whose General Availability is scheduled for March 2027. Until then,
> install an Early Access build from [jdk.java.net/27](https://jdk.java.net/27/) or build with any JDK ≥ 21 by
> passing `-Djava.version=25` (or 21). The code only uses language features available since Java 21.

## Quick start

```bash
git clone https://github.com/AnderCMD/Autto-Framework.git
cd Autto-Framework

# Everything (unit tests + scenarios) with Chrome
./mvnw test

# Smoke scenarios in headless Firefox
./mvnw test -Dbrowser=firefox -Dbrowser.headless=true -Dcucumber.filter.tags=@smoke

# Using an older JDK
./mvnw test -Djava.version=21
```

Open **`target/autto-reports/index.html`** when the run finishes.

## Project structure

```
src
├── main/java/io/github/andercmd/autto/core      ← reusable framework (do not put tests here)
│   ├── config      layered configuration (AuttoConfig, ConfigKeys)
│   ├── driver      browser/device factory, options, thread-safe DriverManager
│   ├── media       screenshots and video recorder
│   ├── report      Extent Cucumber plugin, Report API, report folders
│   ├── ui          BasePage with explicit waits
│   ├── data        JSON test data + Datafaker
│   └── context     ScenarioContext shared between steps
└── test
    ├── java/io/github/andercmd/autto
    │   ├── CucumberTestSuite.java              ← entry point
    │   ├── shared/hooks/BrowserHooks.java      ← browser life cycle + evidence
    │   └── features                            ← ONE FOLDER PER BUSINESS FEATURE
    │       ├── login/      LoginPage, LoginSteps, Credentials
    │       ├── inventory/  InventoryPage, InventorySteps
    │       ├── checkout/   CartPage, CheckoutPage, CheckoutSteps, Customer
    │       └── showcase/   report demo without browser
    └── resources
        ├── autto.properties                    ← main configuration
        ├── environments/{qa,staging,prod}.properties
        ├── features/<feature>/*.feature        ← Gherkin, same folders as the code
        ├── testdata/<feature>/*.json
        ├── junit-platform.properties           ← Cucumber options
        └── logback-test.xml
```

## Documentation

| Guide | Description |
|---|---|
| [Getting started](docs/en/getting-started.md) | Requirements, installation, first run, IDE setup |
| [Architecture](docs/en/architecture.md) | Screaming architecture, layers, execution flow |
| [Writing tests](docs/en/writing-tests.md) | Add a new feature step by step, page objects, data, context |
| [Configuration](docs/en/configuration.md) | Every configuration key |
| [Running tests](docs/en/running-tests.md) | Browsers, tags, parallelism, Grid, Docker, cloud, Appium |
| [Reports & evidence](docs/en/reporting.md) | Extent report, screenshots, videos, Report API |
| [CI/CD](docs/en/ci-cd.md) | GitHub Actions, Jenkins, GitLab, Azure DevOps |
| [Troubleshooting](docs/en/troubleshooting.md) | Common problems and fixes |
| [Contributing](CONTRIBUTING.md) | How to contribute |

## Demo application

The sample scenarios target [saucedemo.com](https://www.saucedemo.com), a public demo store. Replace the
`features/*` folders with your own application's features and change `base.url` in `environments/*.properties`.
The `@demo-failure` scenario fails on purpose; run it to see failure evidence in the report:

```bash
./mvnw test -Dcucumber.filter.tags=@demo-failure
```

## License

[MIT](LICENSE) © AnderCMD and contributors.
