# Configuration

[← Back to README](../../README.md) · [Español](../es/configuracion.md)

Configuration lives in `autto-e2e/src/test/resources/application.yml` and is bound to the typed
`AuttoProperties` record. IntelliJ IDEA and VS Code (Spring Boot tools) auto-complete every `autto.*` key with its
description thanks to the metadata shipped in `autto-core`.

## Precedence

Highest first:

1. JVM system properties — `./mvnw test -Dautto.browser.name=firefox`
2. Environment variables — `AUTTO_BROWSER_NAME=firefox` (relaxed binding: dots and dashes become `_`)
3. `.env` file (git-ignored) — see [Secrets](secrets.md)
4. `application-<profile>.yml` for each active profile (the last profile wins)
5. `application.yml`
6. Built-in defaults of `AuttoProperties`

Invalid values stop the run immediately with the offending key, e.g.
`Invalid Autto configuration: ... autto.evidence.video-fps must be between 1 and 30`.

## Profiles

```bash
./mvnw test -Dspring.profiles.active=staging            # one environment
./mvnw test -Dspring.profiles.active=qa,ci,grid         # environment + CI settings + Selenium Grid
SPRING_PROFILES_ACTIVE=qa,docker ./mvnw test            # environment variable (or in .env)
```

| Profile | Purpose |
|---|---|
| `qa` (default) | QA environment URL |
| `staging`, `prod` | Other environments (`prod` disables videos) |
| `ci` | Headless browsers |
| `docker` | Browsers in disposable Docker containers (WebDriverManager) |
| `grid` | Selenium Grid (`docker compose up -d`) |
| `browserstack` | BrowserStack (credentials from `.env` / CI secrets) |

Create `application-local.yml` (git-ignored) for personal settings and run with `-Dspring.profiles.active=qa,local`.

## Reference

Durations accept `500ms`, `15s`, `5m`. Enums accept `on-failure`, `ON_FAILURE`, `onFailure`.

### `autto` (root)

| Key | Default | Description |
|---|---|---|
| `autto.base-url` | — | Base URL used by `BasePage.open("/path")`. |

### `autto.browser`

| Key | Default | Description |
|---|---|---|
| `name` | `chrome` | `chrome`, `chromium`, `firefox`, `edge`, `safari`. |
| `version` | installed | Exact version or channel (`stable`, `beta`, `dev`, `canary`); uses Selenium Manager, which can download it. |
| `headless` | `false` | Headless mode (ignored by Safari). |
| `window-size` | `1920x1080` | `maximized` or `<width>x<height>`. |
| `args` | `[]` | Extra command-line arguments. |
| `binary` | — | Custom browser executable (Brave, Chromium builds, Firefox Developer Edition…). |
| `incognito` | `false` | Private / incognito window. |
| `mobile-emulation` | — | Chromium device emulation, e.g. `iPhone 14 Pro Max`. |
| `accept-insecure-certs` | `true` | Accept self-signed certificates. |
| `page-load-strategy` | `normal` | `normal`, `eager`, `none`. |
| `download-dir` | `target/downloads` | Download folder (Chromium and Firefox). |
| `unhandled-prompt` | `ignore` while recording | `accept`, `dismiss`, `accept and notify`, `dismiss and notify`, `ignore`. |
| `console-logs` | `true` | Collect console messages and JavaScript errors (WebDriver BiDi), attached on failure. |
| `prefs.<name>` | — | Browser preferences, e.g. `intl.accept_languages: es-ES`. |

### `autto.driver`

| Key | Default | Description |
|---|---|---|
| `resolution` | `webdrivermanager` | `webdrivermanager` or `selenium-manager`. |
| `fallback` | `true` | Use Selenium Manager when WebDriverManager fails. |
| `docker-fallback` | `false` | Start the browser in Docker when it is not installed locally (requires Docker). |
| `cache-path` | `~/.cache/selenium` | WebDriverManager driver cache. |
| `start-retries` | `1` | Extra attempts when the browser session cannot be created (busy Grid, cloud queue, slow Docker). Configuration errors are never retried. `0`–`10`. |
| `start-retry-delay` | `2s` | Pause before the first retry; doubled after every failed attempt. |

WebDriverManager also reads its own `wdm.*` system properties / `WDM_*` variables (proxy, mirrors, timeouts), e.g.
`-Dwdm.proxy=proxy.company.com:8080`.

### `autto.execution`

| Key | Default | Description |
|---|---|---|
| `target` | `local` | `local`, `docker`, `remote` (Grid / cloud) or `appium`. |
| `remote-url` | `http://localhost:4444` | Grid or vendor hub. Credentials in the URL are redacted in logs and reports. |
| `platform-name` | — | Requested OS for remote sessions (`Windows 11`, `macOS 15`, `linux`). |
| `capabilities` | `{}` | Any W3C / vendor capability (see below). |

```yaml
autto:
  execution:
    capabilities:
      se:recordVideo: true              # vendor names with ':' work as-is ("[se:recordVideo]" also accepted)
      bstack:options:
        os: Windows
        osVersion: '"11"'               # inner quotes keep it a string
      goog:chromeOptions:
        args: [--lang=es]
```

### `autto.docker` (target `docker`)

| Key | Default | Description |
|---|---|---|
| `vnc` | `false` | Expose a noVNC URL (printed in the log) to watch the browser. |
| `screen-resolution` | `1920x1080x24` | Virtual screen. |
| `shm-size` | `2g` | Shared memory of the container. |

### `autto.appium` (target `appium`)

| Key | Default | Description |
|---|---|---|
| `url` | `http://127.0.0.1:4723` | Appium server. |
| `platform` | `android` | `android` or `ios`. |
| `app` | — | `.apk` / `.ipa` / `.app` path or URL. Omit for mobile web. |

### `autto.timeouts`

| Key | Default | Description |
|---|---|---|
| `implicit` | `0s` | Keep it at 0: the framework uses explicit waits. |
| `explicit` | `15s` | Default explicit wait of `BasePage`. |
| `page-load` | `60s` | Page load timeout. |
| `script` | `30s` | Asynchronous script timeout. |
| `polling` | `250ms` | Polling interval of explicit waits. |

### `autto.evidence`

| Key | Default | Description |
|---|---|---|
| `screenshot` | `on-failure` | `off`, `on-failure`, `always` (after every step). |
| `video` | `on-failure` | `off`, `on-failure` (recorded always, kept only for failures), `always`. |
| `video-fps` | `3` | 1–30. |
| `video-max-duration` | `5m` | Only the last part of long scenarios is kept. |
| `video-max-width` | `1280` | Frames are downscaled to this width. |
| `page-source` | `true` | Attach the page HTML when a scenario fails. |

### `autto.report`

| Key | Default | Description |
|---|---|---|
| `dir` | `target/autto-reports` | Output folder (relative to the module). |
| `timestamped` | `false` | One sub-folder per run. |
| `title` / `name` | Autto… | Browser tab title / header name. |
| `theme` | `dark` | `dark` or `standard`. |
| `offline` | `true` | Copy assets so the report opens without internet. |
| `timeline` | `true` | Timeline chart. |
| `screenshots-base64` | `false` | Embed screenshots in the HTML. |
| `author` | — | Default author when a scenario has no `@author:<name>` tag. |
| `show-host` | `true` | Show user and host in the dashboard. |
| `info.<label>` | — | Extra dashboard rows. |

### `autto.api` (REST client, see [API testing](api-testing.md))

| Key | Default | Description |
|---|---|---|
| `base-url` | — | Base URL of the API under test; requests may also use absolute URLs. |
| `connect-timeout` | `10s` | TCP connection timeout. |
| `read-timeout` | `30s` | Socket read timeout. |
| `relaxed-https` | `false` | Trust any certificate (test environments with self-signed certificates only). |
| `report` | `true` | Attach every request and response to the report (sensitive headers and secrets masked). |
| `headers.<name>` | — | Default headers, e.g. `Authorization: Bearer ${API_TOKEN}`. |

### `autto.accessibility` (axe-core audits)

| Key | Default | Description |
|---|---|---|
| `tags` | `[wcag2a, wcag2aa, wcag21a, wcag21aa]` | axe rule tags to evaluate (`wcag22aa`, `best-practice`…). |
| `disabled-rules` | `[]` | axe rule ids that are never evaluated (e.g. `color-contrast` while a redesign is pending). |
| `fail-on` | `serious` | Minimum impact rejected by `AccessibilityResult.assertNoViolations()`: `minor`, `moderate`, `serious`, `critical`. |

### `autto.scenario`, `autto.performance`, `autto.visual` ([Advanced testing](advanced-testing.md))

| Key | Default | Description |
|---|---|---|
| `scenario.timeout` | `10m` | A scenario running longer is aborted (browser closed, thread interrupted); `0` disables. |
| `performance.fcp` / `lcp` / `ttfb` / `load` | `1800ms` / `2500ms` / `800ms` / `5s` | Budgets of `WebPerformance`; `0` disables one. |
| `performance.cls` | `0.1` | Cumulative layout shift budget; `0` disables. |
| `visual.baseline-dir` | `src/test/resources/visual` | Approved baseline images. |
| `visual.tolerance` | `0.001` | Share of different pixels (0-1) still accepted. |
| `visual.pixel-threshold` | `10` | Per-channel difference (0-255) below which two pixels are equal. |
| `visual.update` | `false` | Create / replace baselines instead of comparing (`-Dautto.visual.update=true`). |
| `visual.diff-dir` | `target/autto-reports/visual` | Actual and diff images of failed comparisons. |

### `autto.db`, `autto.mail`, `autto.notifications`

| Key | Default | Description |
|---|---|---|
| `db.url` / `username` / `password` | — | JDBC connection of the `Database` helper (the password is masked). `driver-class` only for drivers that do not self-register. |
| `mail.url` | `http://localhost:8025` | Mailpit-compatible API read by `Mailbox`. |
| `mail.timeout` | `30s` | How long `Mailbox.waitFor` polls. |
| `notifications.webhook-url` | — | Slack / Teams / generic incoming webhook (a secret); empty disables notifications. |
| `notifications.type` | `slack` | `slack`, `teams` or `generic` (JSON metrics). |
| `notifications.only-on-failure` | `false` | Send only when a scenario failed. |
| `notifications.report-url` | — | Link added to the message (CI run, GitHub Pages). |

### Cucumber (`junit-platform.properties`)

| Key | Default | Description |
|---|---|---|
| `cucumber.filter.tags` | `not @wip and not @ignore and not @demo-failure` | Tag expression. |
| `cucumber.features` | — | Override selected features (`classpath:features/login`, comma separated; a failed-scenario list: `-Dcucumber.features="$(paste -sd, autto-e2e/target/autto-reports/rerun.txt)"` or `./scripts/run-with-rerun.sh`). |
| `cucumber.glue` | `io.github.andercmd.autto.e2e,io.github.andercmd.autto.core.cucumber` | Steps + framework hooks. |
| `cucumber.execution.parallel.enabled` | `false` | Parallel scenarios. |
| `cucumber.execution.parallel.config.strategy` | `dynamic` | `dynamic` (cores × factor) or `fixed` (`fixed.parallelism=N`, best when a Grid or cloud limits the free sessions). |
| `cucumber.execution.parallel.config.dynamic.factor` | `1` | Threads per core. |

### Other

| Key | Default | Description |
|---|---|---|
| `autto.dotenv.path` / `AUTTO_DOTENV_PATH` | auto | Explicit `.env` location. |
| `autto.log.level` | `INFO` | Log level of framework and tests (`DEBUG` traces every interaction). |
| `autto.ignoreFailures` | `false` | Keep the Maven build green when scenarios fail. |
| `checkstyle.skip` | `false` | Skip the coding-standard check (not recommended). |
