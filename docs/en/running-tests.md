# Running tests

[← Back to README](../../README.md) · [Español](../es/ejecucion.md)

> On Windows use `mvnw.cmd` instead of `./mvnw`. In PowerShell quote `-D` arguments: `"-Dbrowser=firefox"`.

## Selecting scenarios

```bash
./mvnw test                                                   # everything (default filter excludes @wip, @ignore, @demo-failure)
./mvnw test -Dcucumber.filter.tags="@smoke"
./mvnw test -Dcucumber.filter.tags="@regression and not @slow"
./mvnw test -Dcucumber.features=classpath:features/login      # one folder
./mvnw test -Dcucumber.features=classpath:features/login/login.feature:12   # one scenario (line)
./mvnw test -Dcucumber.filter.name="Successful login"         # by name (partial match)
./mvnw -Punit test                                            # only the framework unit tests
```

Re-run only the failures of the previous run:

```bash
./mvnw test -Dcucumber.features=@target/autto-reports/rerun.txt
```

## Browsers

```bash
./mvnw test -Dbrowser=chrome
./mvnw test -Dbrowser=firefox
./mvnw test -Dbrowser=edge
./mvnw test -Dbrowser=safari          # macOS, run `safaridriver --enable` once
./mvnw test -Dbrowser=chrome -Dbrowser.version=beta
./mvnw test -Dbrowser.headless=true -Dbrowser.window.size=1366x768
./mvnw test -Dbrowser.mobile.emulation="iPhone 14 Pro Max"
```

Drivers are resolved by Selenium Manager. When the requested browser is missing (or a specific version is
requested), Selenium Manager can download Chrome or Firefox into `~/.cache/selenium`.

## Parallel execution

Every scenario gets its own browser, video recorder, report node and `ScenarioContext`.

```bash
./mvnw test -Dcucumber.execution.parallel.enabled=true -Dcucumber.execution.parallel.config.fixed.parallelism=4
```

Scenarios that must not run at the same time can be serialized with exclusive resources, e.g.
`cucumber.execution.exclusive-resources.<tag>.read-write=<resource>` in `junit-platform.properties`.

## Platforms

### Local (Windows, macOS, Linux)

Default (`execution.target=local`). Nothing to install besides the browser.

### Selenium Grid with Docker

```bash
docker compose up -d                       # hub + Chrome, Firefox and Edge nodes
./mvnw test -Dexecution.target=remote -Dremote.url=http://localhost:4444 -Dbrowser=firefox
docker compose down
```

Watch the sessions live at <http://localhost:4444/ui> (VNC password `secret`).

### Cloud providers

Any W3C-compliant vendor works with `execution.target=remote`, a hub URL and vendor capabilities.

**BrowserStack**

```properties
execution.target=remote
remote.url=https://${BROWSERSTACK_USERNAME}:${BROWSERSTACK_ACCESS_KEY}@hub-cloud.browserstack.com/wd/hub
browser=chrome
capabilities.bstack:options.os=Windows
capabilities.bstack:options.osVersion="11"
capabilities.bstack:options.projectName=Autto
capabilities.bstack:options.buildName=${GITHUB_RUN_ID:local}
```

**Sauce Labs**

```properties
execution.target=remote
remote.url=https://${SAUCE_USERNAME}:${SAUCE_ACCESS_KEY}@ondemand.eu-central-1.saucelabs.com:443/wd/hub
browser=edge
platform.name=Windows 11
capabilities.sauce:options.name=Autto
```

**LambdaTest**

```properties
execution.target=remote
remote.url=https://${LT_USERNAME}:${LT_ACCESS_KEY}@hub.lambdatest.com/wd/hub
browser=firefox
capabilities.LT:Options.platformName=macOS Sequoia
capabilities.LT:Options.build=Autto
```

Put those lines in an environment file (e.g. `environments/browserstack.properties`) and run `-Denv=browserstack`.

### Mobile (Appium 2+)

```bash
npm i -g appium && appium driver install uiautomator2   # Android
appium                                                   # start the server
```

Mobile web (Chrome on Android):

```properties
execution.target=appium
appium.platform=android
capabilities.browserName=Chrome
capabilities.appium:deviceName=Pixel 8
```

Native app:

```properties
execution.target=appium
appium.platform=ios
appium.app=/path/to/MyApp.app
capabilities.appium:deviceName=iPhone 16
capabilities.appium:platformVersion="18.0"
```

The `AndroidDriver` / `IOSDriver` returned by `DriverManager.driver()` can be cast when you need mobile specific
commands. Screenshots, videos and reports work the same way.

## Useful flags

| Flag | Effect |
|---|---|
| `-Dautto.ignoreFailures=true` | Do not fail the Maven build when scenarios fail (reports are always written). |
| `-Dautto.log.level=DEBUG` | Log every click, typing and wait. |
| `-Dvideo.mode=always` | Keep the video of every scenario. |
| `-Dscreenshot.mode=always` | Screenshot after every step. |
| `-Dreport.timestamped=true` | Keep one report folder per run. |
