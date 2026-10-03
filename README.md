<div align="center">

# Autto Framework

**Enterprise, open source QA automation framework**
**Spring Boot · Cucumber · Selenium · REST Assured · axe-core · Extent Reports · Java 27**

[![CI](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml/badge.svg)](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-27-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6db33f)
![Cucumber](https://img.shields.io/badge/Cucumber-8.0.4-23d96c)
![Selenium](https://img.shields.io/badge/Selenium-4.50.0-43b02a)

English · [Español](README.es.md)

</div>

---

Autto is a production-grade starting point for UI, API and accessibility test automation, for a single tester
or a whole company. The engine (`autto-core`) is a reusable Spring
Boot auto-configuration; the test suite (`autto-e2e`) only contains business features. Browsers, drivers, secrets,
parallelism, evidence and reports are already solved.

## Highlights

| | |
|---|---|
| **Spring Boot** | Dependency injection with one instance per scenario (`@PageObject`), profiles per environment, typed and validated configuration with IDE auto-completion. |
| **Screaming architecture** | Tests organized by business feature (`features/login`, `features/checkout`…), engine and suite in separate modules. |
| **Any browser, no matter what** | Chrome, Chromium, Firefox, Edge, Safari. WebDriverManager → Selenium Manager fallback → Docker fallback when the browser is not installed. |
| **Any platform** | Windows, macOS, Linux, Docker browsers, Selenium Grid, BrowserStack / Sauce Labs / LambdaTest, Android and iOS (Appium). |
| **Secure secrets** | `.env` (git-ignored) locally, CI secrets in pipelines, values masked in logs and reports, gitleaks in CI and pre-commit. |
| **API testing** | REST Assured `Api` bean with base URL, default headers and timeouts; every request and response in the report with secrets masked. |
| **Accessibility** | axe-core audits against WCAG 2.1 A/AA with one call (`Accessibility.scan()`), violations listed in the report. |
| **Resilient by default** | Browser start retries with back-off, explicit waits only, stale elements retried, scenario timeout, driver self-healing after browser updates, failed scenarios re-run once and flaky ones reported, soft assertions. |
| **Beyond clicks** | Database helper with automatic clean-up, e-mail / one-time code verification (Mailpit, TOTP), JSON-schema contract tests, performance budgets, visual regression, network mocking (experimental), unique test data. |
| **Observable** | `metrics.json` / Prometheus file, Slack and Teams summaries, correlation id in logs and API calls, JSON logs, nightly reports published with history. |
| **Rich reports** | Extent Spark dashboard, timeline, tags, devices, authors, failures-only report; Cucumber HTML/JSON/JUnit outputs. |
| **Evidence** | Screenshots, **MP4 video of every scenario** (no ffmpeg), page source, browser console (BiDi), attachments. |
| **Quality gates** | Maven Enforcer, Checkstyle (no `Thread.sleep`, no `System.out`…), unit tests, JaCoCo coverage gate, SpotBugs, Dependabot. |
| **CI ready** | GitHub Actions pinned by SHA: secret scanning, build on JDK 21/25/27, fast pull-request matrix and full cross-OS × cross-browser matrix, nightly Grid and Docker regressions, OSV scans. |
| **Supply chain** | CycloneDX SBOM, build-provenance attestation, Sigstore signatures, GitHub releases and Maven Central profile. |
| **Easy to start** | `scripts/new-project.sh` creates a working project, dev container, `scripts/doctor.sh` diagnoses an environment. |

## Tech stack

| Component | Version |
|---|---|
| Java | 27 (configurable through `java.version`, minimum 21) |
| Spring Boot | 4.1.1 |
| Cucumber JVM (+ cucumber-spring) | 8.0.4 |
| Selenium | 4.50.0 |
| WebDriverManager | 6.4.0 |
| Appium Java client | 10.1.1 |
| JUnit Platform | 6.1.3 |
| REST Assured (+ JSON Schema validator) | 6.0.1 |
| axe-core (Selenium) | 4.13.0 |
| Extent Reports | 5.1.2 |
| Checkstyle | 14.3.0 |
| Maven (wrapper) | 3.10.0 |

> **About Java 27.** Its General Availability is scheduled for March 2027. Until then install an
> [Early Access build](https://jdk.java.net/27/) or build with any JDK ≥ 21 using `-Djava.version=25` (or 21).

## Quick start

```bash
git clone https://github.com/AnderCMD/Autto-Framework.git
cd Autto-Framework

cp .env.example .env              # Windows: copy .env.example .env
# edit .env → SAUCE_PASSWORD=secret_sauce   (public password of the demo store)

./mvnw install                                                            # everything, Chrome
./mvnw -pl autto-e2e test -Dautto.browser.name=firefox -Dcucumber.filter.tags=@smoke
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,docker             # browsers in Docker
./mvnw install -Djava.version=21                                          # older JDK
```

Open **`autto-e2e/target/autto-reports/index.html`**.

## Project structure

```
Autto-Framework
├── pom.xml                         parent: versions, plugins, quality gates
├── .env.example                    secret NAMES (copy to .env, which is git-ignored)
├── config/checkstyle/              coding standard (config/spotbugs/ for the quality profile)
├── scripts/                        doctor, rerun of failures, new-project, report publishing
├── templates/starter/              project template used by scripts/new-project.sh
├── .devcontainer/                  one-click development environment
├── docker-compose.yml              Selenium Grid
├── autto-core/                     ENGINE (publishable library, Spring Boot auto-configuration)
│   └── src/main/java/io/github/andercmd/autto/core/
│       ├── config/     AuttoProperties (typed), AuttoSettings, DotEnv, profiles
│       ├── spring/     AuttoAutoConfiguration
│       ├── driver/     DriverResolver (WebDriverManager → Selenium Manager → Docker), DriverFactory, DriverManager
│       ├── api/        Api (REST Assured), ApiReportFilter
│       ├── a11y/       Accessibility (axe-core), AccessibilityResult
│       ├── ui/         BasePage, @PageObject
│       ├── cucumber/   BrowserHooks (browser life cycle + evidence), timeout, tag and resource hooks
│       ├── db/ mail/ network/ perf/ visual/   Database, Mailbox, NetworkMock, WebPerformance, VisualRegression
│       ├── observability/   RunSummaryPlugin (metrics, notifications), correlation id
│       ├── doctor/     environment self-check
│       ├── media/      screenshots, video recorder
│       ├── report/     Extent Cucumber plugin, Report API
│       ├── security/   Secrets masking, Credentials
│       ├── data/       JSON/YAML test data, Datafaker
│       └── context/    ScenarioContext
└── autto-e2e/                      TEST SUITE of the application under test
    └── src/test/
        ├── java/io/github/andercmd/autto/e2e/
        │   ├── CucumberTestSuite · CucumberSpringConfiguration · E2eTestApplication
        │   ├── shared/TestUsers.java
        │   └── features/           ONE FOLDER PER BUSINESS FEATURE
        │       ├── login/      LoginPage, LoginSteps
        │       ├── inventory/  InventoryPage, InventorySteps
        │       ├── checkout/   CartPage, CheckoutPage, CheckoutSteps, Customer
        │       ├── api/        StorefrontApiSteps (API + soft assertions)
        │       ├── accessibility/ AccessibilitySteps (axe-core)
        │       └── showcase/   ReportShowcaseSteps (Report API demo)
        └── resources/
            ├── application.yml + application-{qa,staging,prod,ci,docker,grid,browserstack}.yml
            ├── features/<feature>/*.feature
            ├── junit-platform.properties
            └── logback-test.xml
```

## Documentation

| Guide | Description |
|---|---|
| [Getting started](docs/en/getting-started.md) | Requirements, first run, IDE setup |
| [Adoption guide](docs/en/adoption.md) | Starter, team and enterprise setups; using `autto-core` from your own project |
| [Architecture](docs/en/architecture.md) | Modules, screaming architecture, dependency injection, execution flow |
| [Architecture decisions](docs/en/decisions.md) | Why Spring Boot, WebDriverManager, `.env`, multi-module, REST Assured, axe-core |
| [Writing tests](docs/en/writing-tests.md) | New feature step by step, page objects, soft assertions, data, accessibility |
| [Advanced testing](docs/en/advanced-testing.md) | Stability, database, e-mail, contracts, performance, visual regression, observability, supply chain |
| [API testing](docs/en/api-testing.md) | `Api` client, authentication, report, data set-up for UI scenarios |
| [Configuration](docs/en/configuration.md) | Profiles, precedence and every `autto.*` key |
| [Secrets & environment variables](docs/en/secrets.md) | `.env`, CI secrets, masking, leak prevention |
| [Running tests](docs/en/running-tests.md) | Tags, browsers, parallelism, Docker, Grid, cloud, Appium |
| [Reports & evidence](docs/en/reporting.md) | Extent report, screenshots, videos, Report API |
| [CI/CD](docs/en/ci-cd.md) | GitHub Actions, Jenkins, GitLab, Azure DevOps |
| [Troubleshooting](docs/en/troubleshooting.md) | Common problems and fixes |
| [Contributing](CONTRIBUTING.md) | How to contribute |

## Demo application

The sample scenarios target [saucedemo.com](https://www.saucedemo.com). Replace `features/*`, `test-data.users` and
`autto.base-url` with your application. The `@demo-failure` scenario fails on purpose to showcase failure evidence:

```bash
./mvnw -pl autto-e2e test -Dcucumber.filter.tags=@demo-failure
```

## License

[MIT](LICENSE) © AnderCMD and contributors.
