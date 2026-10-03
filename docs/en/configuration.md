# Configuration

[← Back to README](../../README.md) · [Español](../es/configuracion.md)

## Resolution order

Every value is looked up in four layers; the last one wins:

1. `src/test/resources/autto.properties` — project defaults
2. `src/test/resources/environments/<env>.properties` — environment values (`env=qa` by default)
3. Environment variables prefixed with `AUTTO_` — `browser.headless` → `AUTTO_BROWSER_HEADLESS`
4. JVM system properties — `./mvnw test -Dbrowser.headless=true`

Blank values are ignored, so an empty `-Dbrowser=` never erases a configured value. Values support
`${placeholders}` (see [Placeholders](#placeholders)).

```bash
# Select another environment file
./mvnw test -Denv=staging
AUTTO_ENV=staging ./mvnw test
```

Create `environments/local.properties` (git-ignored) for personal overrides and run with `-Denv=local`.

## Reference

### General

| Key | Default | Description |
|---|---|---|
| `env` | `qa` | Environment file to load from `environments/`. |
| `base.url` | — | Base URL used by `BasePage.open("/path")`. |

### Browser

| Key | Default | Description |
|---|---|---|
| `browser` | `chrome` | `chrome`, `firefox`, `edge`, `safari`. |
| `browser.version` | installed | Exact version or channel (`stable`, `beta`, `dev`, `canary`). Selenium Manager downloads it if needed. |
| `browser.headless` | `false` | Headless mode (ignored by Safari). |
| `browser.window.size` | `1920x1080` | `maximized` or `<width>x<height>`. |
| `browser.args` | — | Extra command-line arguments, comma separated. |
| `browser.binary` | — | Custom browser binary (Chromium, Brave, Firefox Developer Edition…). |
| `browser.incognito` | `false` | Private/incognito window. |
| `browser.mobile.emulation` | — | Chromium device emulation, e.g. `iPhone 14 Pro Max`, `Pixel 7`. |
| `browser.accept.insecure.certs` | `true` | Accept self-signed certificates. |
| `browser.page.load.strategy` | `normal` | `normal`, `eager`, `none`. |
| `browser.download.dir` | `target/downloads` | Download folder (Chromium and Firefox). |
| `browser.unhandled.prompt` | `ignore` if video is on | `accept`, `dismiss`, `accept and notify`, `dismiss and notify`, `ignore`. |
| `browser.console.logs` | `true` | Collect console messages and JS errors through WebDriver BiDi and attach them on failure. |
| `browser.prefs.<name>` | — | Browser preferences (Chromium `prefs` / Firefox profile), e.g. `browser.prefs.intl.accept_languages=es-ES`. |

### Execution target

| Key | Default | Description |
|---|---|---|
| `execution.target` | `local` | `local`, `remote` (Grid / cloud) or `appium`. |
| `remote.url` | `http://localhost:4444` | Selenium Grid or vendor hub URL. Credentials in the URL are redacted in logs. |
| `platform.name` | — | Requested OS for remote sessions (`Windows 11`, `macOS 15`, `linux`). |
| `capabilities.<name>` | — | Any W3C / vendor capability, see below. |
| `appium.url` | `http://127.0.0.1:4723` | Appium server. |
| `appium.platform` | `android` | `android` or `ios`. |
| `appium.app` | — | Path or URL of the `.apk` / `.ipa` / `.app`. Omit it for mobile web. |

#### Capabilities

Flat keys become nested JSON. Values are converted to booleans/numbers unless quoted; `[a,b]` is a list.

```properties
capabilities.se:recordVideo=true
capabilities.bstack:options.os=Windows
capabilities.bstack:options.osVersion="11"
capabilities.goog:chromeOptions.args=[--lang=es]
capabilities.appium:deviceName=Pixel 8
```

#### Placeholders

Any value can reference another key or an environment variable with `${NAME}` or `${NAME:default}`. This keeps
credentials out of the repository:

```properties
remote.url=https://${BROWSERSTACK_USERNAME}:${BROWSERSTACK_ACCESS_KEY}@hub-cloud.browserstack.com/wd/hub
capabilities.bstack:options.buildName=${GITHUB_RUN_ID:local-build}
```

A placeholder without default that cannot be resolved fails with a clear message when the key is read.

### Timeouts (seconds)

| Key | Default | Description |
|---|---|---|
| `timeouts.implicit` | `0` | Implicit wait. Keep 0; the framework uses explicit waits. |
| `timeouts.explicit` | `15` | Default explicit wait of `BasePage`. |
| `timeouts.page.load` | `60` | Page load timeout. |
| `timeouts.script` | `30` | Async script timeout. |
| `timeouts.polling.ms` | `250` | Polling interval of explicit waits (milliseconds). |

### Evidence

| Key | Default | Description |
|---|---|---|
| `screenshot.mode` | `on_failure` | `off`, `on_failure`, `always` (after every step). |
| `video.mode` | `on_failure` | `off`, `on_failure` (recorded always, kept only for failures), `always`. |
| `video.fps` | `3` | Frames per second (1–30). Higher values cost more WebDriver calls. |
| `video.max.seconds` | `300` | Only the last N seconds of a scenario are kept. |
| `video.max.width` | `1280` | Frames are downscaled to this width. |
| `evidence.page.source` | `true` | Attach the HTML of the page when a scenario fails. |

### Report

| Key | Default | Description |
|---|---|---|
| `report.dir` | `target/autto-reports` | Output folder of the Extent report and evidence. |
| `report.timestamped` | `false` | One sub-folder per run (`yyyyMMdd-HHmmss`) to keep history. |
| `report.title` | `Autto · Test Automation Report` | Browser tab title. |
| `report.name` | `Autto Framework · Execution report` | Name shown in the header. |
| `report.theme` | `dark` | `dark` or `standard`. |
| `report.offline` | `true` | Copy report assets locally so it opens without internet. |
| `report.timeline` | `true` | Timeline chart in the dashboard. |
| `report.screenshots.base64` | `false` | Embed screenshots inside the HTML (portable single file, heavier). |
| `report.author` | — | Default author when a scenario has no `@author:<name>` tag. |
| `report.show.host` | `true` | Show user and host name in the dashboard. |
| `report.info.<label>` | — | Extra rows for the dashboard environment table. |

### Cucumber (`junit-platform.properties`)

| Key | Default | Description |
|---|---|---|
| `cucumber.filter.tags` | `not @wip and not @ignore and not @demo-failure` | Tag expression. |
| `cucumber.features` | — | Override selected features, e.g. `classpath:features/login`. |
| `cucumber.glue` | `io.github.andercmd.autto` | Packages scanned for steps and hooks. |
| `cucumber.plugin` | Extent + HTML + JSON + JUnit + NDJSON + rerun | Report plugins. |
| `cucumber.execution.parallel.enabled` | `false` | Run scenarios in parallel. |
| `cucumber.execution.parallel.config.fixed.parallelism` | `4` | Parallel threads. |

### Logging

| Key | Default | Description |
|---|---|---|
| `autto.log.level` | `INFO` | Log level of framework and test classes (`DEBUG` shows every click/type). |
