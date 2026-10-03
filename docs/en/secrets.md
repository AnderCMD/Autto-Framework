# Secrets & environment variables

[← Back to README](../../README.md) · [Español](../es/secretos.md)

> Why `.env` and not something else? See [ADR-003 in Architecture decisions](decisions.md).

## Local development

```bash
cp .env.example .env          # Windows: copy .env.example .env
```

Edit `.env` (it is git-ignored):

```dotenv
SAUCE_PASSWORD=secret_sauce          # demo value published by saucedemo.com
AUTTO_BROWSER_NAME=firefox           # any autto.* setting can be overridden
SPRING_PROFILES_ACTIVE=qa
```

The file is found in the working directory or up to three parent folders, so it works from the repository root,
from a module and from the IDE. Use `-Dautto.dotenv.path=/path/.env` (or `AUTTO_DOTENV_PATH`) to point elsewhere.

Supported syntax: `KEY=value`, `export KEY=value`, `# comments`, `'single quotes'` (literal), `"double quotes"`
(escapes, multi-line).

## Using a secret

Reference it from `application.yml`, never write the value:

```yaml
test-data:
  users:
    admin:
      username: admin@company.com
      password: ${ADMIN_PASSWORD}
```

```java
@Component
public class TestUsers { ... }                          // already included

Credentials admin = users.get("admin");                 // fails with a clear message if ADMIN_PASSWORD is missing
loginPage.loginAs(admin);
```

Other options:

- `AuttoSettings.get().find("ADMIN_PASSWORD")` anywhere in the code.
- `@Value("${ADMIN_PASSWORD}")` or `Environment` in Spring beans.
- `${ADMIN_PASSWORD}` placeholders inside JSON test data (`TestData`).

## Precedence

```
-D system property  >  real environment variable  >  .env  >  application-<profile>.yml  >  application.yml
```

A CI secret can therefore never be shadowed by a stale `.env`.

## CI

Declare the same names as pipeline secrets:

| Platform | Where |
|---|---|
| GitHub Actions | *Settings → Secrets and variables → Actions*, then `env: SAUCE_PASSWORD: ${{ secrets.SAUCE_PASSWORD }}` |
| GitLab CI | *Settings → CI/CD → Variables* (masked + protected) |
| Jenkins | Credentials + `withCredentials([string(credentialsId: 'sauce-password', variable: 'SAUCE_PASSWORD')])` |
| Azure DevOps | Variable groups / Key Vault linked variables |

## Masking

Every variable whose **name** looks like a secret (`*PASSWORD*`, `*SECRET*`, `*TOKEN`, `*API_KEY`, `*ACCESS_KEY`,
`*CREDENTIAL*`…) is registered when the run starts. Its value is replaced by `******` in:

- the Extent report (step names, logs, tables, JSON, page sources),
- the execution log (`%maskedMsg` in `logback-test.xml`),
- `Credentials.toString()`.

Register other values manually with `Secrets.register(value)`.

> The native Cucumber reports (`cucumber.html`, `cucumber.json`) are written by Cucumber itself and are not masked:
> never pass secrets as literal step arguments, and do not `scenario.log(...)` them.

## Leak prevention

- `.gitignore`: `.env`, `.env.*` (except `.env.example`), `application-local.yml`.
- CI job **Secret scanning (gitleaks)** scans the full git history on every push and pull request.
- Local hook: `pip install pre-commit && pre-commit install` runs gitleaks before each commit.
- If a secret leaks anyway: **rotate it first**, then clean the history.

## Going further: secret managers

For companies, keep `.env` for local work and load the rest from a vault. Because the framework reads standard
Spring properties, the same names work with Spring Cloud Vault, Azure Key Vault, AWS Secrets Manager or GCP Secret
Manager starters, or with CLI injection (`op run --env-file=.env -- ./mvnw test`, `doppler run -- ./mvnw test`).
