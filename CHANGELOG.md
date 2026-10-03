# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- **Stability**: stale elements are retried inside `BasePage` interactions (`retryStale`); `autto.scenario.timeout`
  aborts hung scenarios; driver self-healing when the browser auto-updated after the driver was cached;
  `scripts/run-with-rerun.sh` re-runs failed scenarios once and reports the recovered ones as flaky; parallelism
  follows the machine by default (`dynamic`).
- **Data and services**: `Unique` (collision-free test data), scenario-scoped `Database` (JDBC with automatic
  clean-up), `Mailbox` (Mailpit) and `Totp` for e-mail and two-factor flows, `ApiContract` (JSON Schema), Mailpit
  in `docker-compose.yml` (profile `mail`).
- **Page quality**: `WebPerformance` budgets, `VisualRegression` with page stabilisation, `NetworkMock` (DevTools, BiDi fallback), environment tags `@viewport:WxH`, `@slow-network`, `@offline`.
- **Allure**: `AllureResultsPlugin` writes Allure results (steps, tags, failures, attachments) without the official
  Cucumber 7 adapter, which is incompatible with Cucumber 8.
- **Observability**: `RunSummaryPlugin` (`metrics.json`, Prometheus file, Slack / Teams / generic webhook),
  correlation id in logs and `X-Correlation-Id` of API calls, JSON logs (`autto/logback-json.xml`).
- **Tooling**: `scripts/doctor.sh`, `scripts/new-project.sh` with `templates/starter`, `.devcontainer/`, Appium
  profile and demo feature, web-quality demo feature.
- **Quality and supply chain**: JaCoCo coverage gate, SpotBugs (`-Pquality`), CycloneDX SBOM (`-Psbom`), Maven
  Central profile (`-Prelease`), Release workflow with attestation and Sigstore signatures, OSV nightly scan.
- **CI**: JDK 21/25/27 build matrix, Linux + Chrome only on pull requests, rerun of failures, optional Pages
  publication with history and Slack / Teams summary, every action pinned to a commit SHA.
- Docs: *Advanced testing* guide (English and Spanish), new configuration keys and troubleshooting entries.

### Changed

- `autto.api.relaxed-https` logs a warning when enabled.
- `AuttoProperties` has six new components (`scenario`, `performance`, `visual`, `db`, `mail`, `notifications`): code
  that builds it by hand must pass them (`AuttoProperties.defaults()` is unaffected).
- Surefire appends the JaCoCo agent (`@{argLine}`), so `-Pcoverage` now really collects coverage.

### Fixed

- `-Dcucumber.features=@file` does not work with the JUnit Platform suite: the rerun script passes the failed
  scenarios as a comma separated list instead.

## [1.1.1] - 2026-10-03

### Fixed

- Nightly regression ran the `@demo-failure` scenario (it fails on purpose): the workflow tag expression now keeps
  the default exclusions.
- Browser console logs (WebDriver BiDi) failed on the Docker Selenium Grid because nodes advertised the hub's
  internal IP: `docker-compose.yml` sets `SE_NODE_GRID_URL`.

## [1.1.0] - 2026-10-03

### Added

- **API testing**: REST Assured 6 behind the `Api` bean (`autto.api.*`: base URL, default headers, timeouts,
  relaxed HTTPS). Every request and response is attached to the report, collapsed, with sensitive headers and
  secrets masked (`ApiReportFilter`). Demo feature `features/api`.
- **Accessibility audits** with axe-core 4.13 (`Accessibility.scan()`, `AccessibilityResult.assertNoViolations()`),
  WCAG tags, disabled rules and failing impact configurable in `autto.accessibility.*`. Demo feature
  `features/accessibility`.
- **Soft assertions**: scenario-scoped AssertJ `SoftAssertions` bean verified automatically at the end of every
  scenario (`SoftAssertionHooks`), before the failure evidence is collected.
- **Browser start retries** with exponential back-off (`autto.driver.start-retries`, `autto.driver.start-retry-delay`)
  for busy Grids and cloud queues; configuration errors are never retried.
- `BasePage` helpers: `waitForPageLoad`, `waitForUrlContains`, `waitForText`, `jsClick`, `doubleClick`,
  `pressKeys`, `typeAndSubmit`, `upload`, frames, windows and alerts.
- YAML test data (`TestData.load("x.yml")`) with the same `${NAME}` placeholders as JSON.
- `Report.code(title, content, expanded)` for collapsed blocks.
- JitPack build (`jitpack.yml`) so any project can depend on `autto-core` without a private repository.
- Dependency review job in CI for pull requests (fails on new high/critical vulnerabilities).
- Docs: adoption guide (starter, team, enterprise), API testing guide, ADR-005 to ADR-007, new configuration keys
  and troubleshooting entries (English and Spanish).

### Changed

- AssertJ is now a compile dependency of `autto-core` (needed by the soft assertions bean).

## [1.0.1] - 2026-10-03

### Fixed

- Local browsers failed to start with `Browser version must be set` when WebDriverManager resolved the driver
  (Selenium 4.50 rejects `setBrowserVersion(null)`). Covered by `DriverFactoryTest`.

## [1.0.0] - 2026-10-03

First stable release.

### Added

- **Spring Boot 4.1** as the test platform (`cucumber-spring`): `autto-core` is an auto-configuration, page objects
  are scenario-scoped beans (`@PageObject`), `ScenarioContext` is injected.
- Typed, validated configuration `AuttoProperties` (`autto.*` in `application.yml`) with IDE metadata, Spring
  profiles (`qa`, `staging`, `prod`, `ci`, `docker`, `grid`, `browserstack`).
- **WebDriverManager 6.4** driver resolution with automatic Selenium Manager fallback, Docker fallback for browsers
  that are not installed, new `docker` execution target and `chromium` browser.
- **`.env` support** (git-ignored) with `.env.example`, secret masking in logs and reports (`Secrets`,
  `Credentials`, `%maskedMsg`), `TestUsers` resolved from secrets.
- gitleaks secret scanning in CI and pre-commit hooks, CODEOWNERS.
- Checkstyle quality gate (no `Thread.sleep`, `System.out`, implicit waits…), banned dependencies, JaCoCo profile.
- Multi-module build: `autto-core` (engine) and `autto-e2e` (suite).
- Architecture decision records (`docs/en/decisions.md`, `docs/es/decisiones.md`) and secrets guides.
- Cucumber 8 + JUnit Platform 6 + Selenium 4.50 + Appium 10 template targeting Java 27, Maven Wrapper 3.10.0.
- Extent Spark report generated by a custom, parallel-safe Cucumber plugin; screenshots, MP4 videos, page source,
  browser console logs.
- GitHub Actions matrix (Windows/macOS/Linux × Chrome/Firefox/Edge + Safari), nightly Grid and Docker regressions.

[Unreleased]: https://github.com/AnderCMD/Autto-Framework/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/AnderCMD/Autto-Framework/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/AnderCMD/Autto-Framework/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/AnderCMD/Autto-Framework/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/AnderCMD/Autto-Framework/releases/tag/v1.0.0
