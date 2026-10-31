# Running tests

[← Back to README](../../README.md) · [Español](../es/ejecucion.md)

> Windows: use `mvnw.cmd`. PowerShell: quote `-D` arguments (`"-Dautto.browser.name=firefox"`).
> Commands below use `-pl autto-e2e` after a first `./mvnw install`; use `./mvnw install` to rebuild everything.

## Selecting scenarios

```bash
./mvnw -pl autto-e2e test                                                   # default filter excludes @wip, @ignore, @demo-failure
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@smoke"
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@regression and not @slow"
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@api"                   # API only, no browser (fast)
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@accessibility"         # axe-core audits
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/login
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/login/login.feature:12
./mvnw -pl autto-e2e test -Dcucumber.filter.name="Successful login"
./scripts/run-with-rerun.sh -Dcucumber.filter.tags=@smoke                          # run, then re-run only the failures once
./mvnw -pl autto-core test                                                  # framework unit tests only
```

## Environments (profiles)

```bash
./mvnw -pl autto-e2e test -Dspring.profiles.active=staging
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,ci
```

## Browsers

```bash
./mvnw -pl autto-e2e test -Dautto.browser.name=chrome
./mvnw -pl autto-e2e test -Dautto.browser.name=chromium
./mvnw -pl autto-e2e test -Dautto.browser.name=firefox
./mvnw -pl autto-e2e test -Dautto.browser.name=edge
./mvnw -pl autto-e2e test -Dautto.browser.name=safari                # macOS: run `safaridriver --enable` once
./mvnw -pl autto-e2e test -Dautto.browser.version=beta               # Selenium Manager downloads Chrome Beta
./mvnw -pl autto-e2e test -Dautto.browser.headless=true -Dautto.browser.window-size=1366x768
./mvnw -pl autto-e2e test -Dautto.browser.mobile-emulation="iPhone 14 Pro Max"
./mvnw -pl autto-e2e test -Dautto.browser.binary="/Applications/Brave Browser.app/Contents/MacOS/Brave Browser"
```

### How drivers are resolved ("any browser, no matter what")

1. **WebDriverManager** detects the installed browser, downloads the matching driver and caches it (once per run).
2. If that fails (offline, blocked URL…), **Selenium Manager** takes over automatically.
3. With `autto.driver.docker-fallback=true`, a browser that is **not installed** is started in **Docker**.
4. With the `docker` profile every browser runs in a disposable container; only Docker is needed.

Behind a corporate proxy: `-Dwdm.proxy=proxy.company.com:8080` (WebDriverManager) and `HTTPS_PROXY` (Selenium
Manager).

## Parallel execution

```bash
./mvnw -pl autto-e2e test -Dcucumber.execution.parallel.enabled=true -Dcucumber.execution.parallel.config.fixed.parallelism=4
```

Each scenario owns its browser, video, report node and scenario-scoped Spring beans.

## Platforms

### Local — Windows, macOS, Linux

Default (`autto.execution.target=local`).

### Docker browsers (no installation)

```bash
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,docker -Dautto.browser.name=firefox
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,docker -Dautto.docker.vnc=true      # watch it live (URL in the log)
```

### Selenium Grid

```bash
docker compose up -d
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,grid -Dautto.browser.name=edge
docker compose down
```

Grid console: <http://localhost:4444/ui>.

### Cloud providers

The `browserstack` profile is ready to use; credentials come from `.env` / CI secrets:

```dotenv
BROWSERSTACK_USERNAME=...
BROWSERSTACK_ACCESS_KEY=...
```

```bash
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,browserstack
```

Sauce Labs / LambdaTest: copy `application-browserstack.yml` and change `remote-url` and the vendor capabilities:

```yaml
autto:
  execution:
    target: remote
    remote-url: https://${SAUCE_USERNAME}:${SAUCE_ACCESS_KEY}@ondemand.eu-central-1.saucelabs.com:443/wd/hub
    platform-name: Windows 11
    capabilities:
      sauce:options:
        name: Autto
```

### Mobile (Appium 2+)

```yaml
# application-android.yml
autto:
  execution:
    target: appium
    capabilities:
      browserName: Chrome              # mobile web; omit and set autto.appium.app for native apps
      appium:deviceName: Pixel 8
  appium:
    platform: android
```

```bash
appium &                                # npm i -g appium && appium driver install uiautomator2
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,android
```

## Useful flags

| Flag | Effect |
|---|---|
| `-Dautto.ignoreFailures=true` | Do not fail the build when scenarios fail (reports are always written). |
| `-Dautto.log.level=DEBUG` | Trace every interaction. |
| `-Dautto.evidence.video=always` | Keep the video of every scenario. |
| `-Dautto.evidence.screenshot=always` | Screenshot after every step. |
| `-Dautto.report.timestamped=true` | One report folder per run. |
| `-Djava.version=25` | Build with an older JDK. |
