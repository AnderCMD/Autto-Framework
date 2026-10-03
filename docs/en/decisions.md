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
bumping one version.
