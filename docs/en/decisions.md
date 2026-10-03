# Architecture decisions

[← Back to README](../../README.md) · [Español](../es/decisiones.md)

Lightweight Architecture Decision Records (ADR): what was decided, why, and what it costs.

## ADR-001 · Spring Boot as the test platform — **accepted**

**Context.** A template used by several teams needs dependency injection, per-environment configuration, typed and
validated settings, and an easy way to plug in API clients, database helpers or test-data builders.

**Decision.** Use Spring Boot 4 (`cucumber-spring`) as the test container. `autto-core` is a Spring Boot
*auto-configuration*: depending on it is enough to get the engine.

**Benefits**

- Constructor injection with **one instance per scenario** (`@ScenarioScope`, `@PageObject`) — safe in parallel.
- `application.yml` + **profiles** (`qa`, `staging`, `ci`, `grid`, `docker`, `browserstack`…) combinable at runtime.
- **Typed, validated configuration** (`AuttoProperties`) with IDE auto-completion and fail-fast errors.
- Relaxed binding: every key can be overridden by `AUTTO_*` variables or `-D` properties.
- The whole Spring ecosystem is one dependency away (REST clients, JDBC, messaging, Vault…) for test-data setup
  and back-end checks.

**Costs.** ~1–2 s context start-up once per run and a larger dependency tree. Acceptable for UI suites whose
scenarios take seconds each.

**When NOT to use it.** A tiny one-person suite without environments or integrations; PicoContainer would be enough.

## ADR-002 · WebDriverManager first, Selenium Manager as fallback — **accepted**

**Context.** Selenium Manager (built into Selenium) already resolves drivers, but enterprise networks add proxies,
mirrors, caches and machines without the requested browser.

**Decision.** `autto.driver.resolution=webdrivermanager` (default) with `autto.driver.fallback=true`:

1. WebDriverManager detects the installed browser and downloads/caches the matching driver (proxy, mirror and cache
   aware, resolved once per run).
2. If it fails, Selenium Manager takes over automatically (it can even download Chrome/Firefox).
3. With `autto.driver.docker-fallback=true`, a browser that is not installed starts in Docker.
4. `execution.target=docker` always runs browsers in disposable containers: only Docker is required.

When a specific browser version/channel is requested (`autto.browser.version=beta`) Selenium Manager is used
directly because it can download that browser.

## ADR-003 · Secrets: `.env` locally, secret store in CI — **accepted**

**Context.** Credentials (test users, cloud keys) must be usable by the tests and must never reach the repository.

**Options considered**

| Option | Verdict |
|---|---|
| Values in `application.yml` / JSON | ❌ Ends up in git history. |
| Only OS environment variables | ✅ Secure, but painful locally (many variables, per shell, per IDE). |
| **`.env` (git-ignored) + `.env.example` (committed)** | ✅ Simple for developers, standard in the industry, same names as CI secrets. |
| Secret manager (Vault, AWS/Azure/GCP secret managers, 1Password CLI) | ✅✅ Best for companies: rotation, audit, no files. Recommended as the next step; Spring Cloud Vault / Azure Key Vault plug into the same property names. |

**Decision.** `.env` for local development, CI secrets as environment variables with the **same names**, and a
secret manager when the organization has one. Defence in depth:

- `.env` and `.env.*` are git-ignored (except `.env.example`, which contains no values).
- `application.yml` only contains references: `password: ${SAUCE_PASSWORD}`.
- Real environment variables always override `.env`.
- Secret values are **masked** (`******`) in logs and in the Extent report.
- **gitleaks** runs in CI (whole history) and as a pre-commit hook.

## ADR-004 · Multi-module build — **accepted**

`autto-core` (versioned engine, publishable to a Maven repository) and `autto-e2e` (the suite of one application).
Other teams create their own `*-e2e` project depending on `autto-core`, and engine improvements reach everybody by
bumping one version. See [Adoption guide](adoption.md) for consuming `autto-core` through JitPack or an internal
repository.

## ADR-005 · REST Assured for API testing — **accepted**

**Context.** Real suites mix UI and API: APIs prepare data in seconds, check back-end state and test services
directly. QA teams need it built in, reported and masked like the UI part.

**Options considered**

| Option | Verdict |
|---|---|
| Spring `RestClient` | ✅ Light and already in the ecosystem, but not a testing DSL (no `then().statusCode()`, JSON path assertions). |
| `java.net.http.HttpClient` | ❌ Too low level for test code. |
| **REST Assured** | ✅ De-facto standard of API test automation, known by most QA engineers, fluent given/when/then DSL. |

**Decision.** REST Assured behind the `Api` bean: base URL, default headers, timeouts and a filter that writes every
exchange to the report with sensitive headers and secrets masked. **Cost:** Groovy and Apache HttpClient 4 on the
test classpath (~10 MB), acceptable for a test framework.

## ADR-006 · axe-core for accessibility — **accepted**

**Context.** Accessibility (WCAG 2.1 AA, European Accessibility Act, ADA) is a legal requirement for many companies
and is cheapest to catch in the same suite that already opens every page.

**Decision.** Deque's `axe-core` Selenium integration (`Accessibility.scan()`), the most widely used open source
engine, with WCAG tags and the failing impact configurable in `autto.accessibility.*`. Scans run only where a step asks
for them, so they never slow down other scenarios.

## ADR-007 · Resilience by default — **accepted**

- **Browser start retries** (`autto.driver.start-retries`, exponential back-off): busy Grids and cloud queues fail
  the session creation, not the test. Configuration errors are never retried.
- **Soft assertions** verified automatically at the end of each scenario: all failures in one run.
- **No retries of failed scenarios** inside the run: they hide real bugs. Re-run failures explicitly with
  `rerun.txt` and investigate flakiness.
