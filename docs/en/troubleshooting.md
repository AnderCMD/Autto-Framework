# Troubleshooting

[← Back to README](../../README.md) · [Español](../es/solucion-de-problemas.md)

### `Autto targets Java 27...` (Maven Enforcer)

Your JDK is older than `java.version`. Install JDK 27 (Early Access until March 2027) or build with
`-Djava.version=25` (any value ≥ 21). Make it permanent in `.mvn/maven.config`.

### `SessionNotCreatedException: ... user data directory is already in use` / Chrome crashes on Linux

Usually Chrome cannot start its sandbox (running as root or inside a container). The framework adds
`--no-sandbox --disable-dev-shm-usage` automatically when running as root, in CI (`CI` variable) or in Docker. In
other cases add them with `-Dbrowser.args=--no-sandbox,--disable-dev-shm-usage`.

### `This version of ChromeDriver only supports Chrome version N`

A driver found in the `PATH` (or `webdriver.chrome.driver`) does not match the browser. Remove it and let Selenium
Manager resolve the right one, or pin a browser with `-Dbrowser.version=<version>`.

### Selenium Manager cannot download drivers (corporate proxy / offline)

Set the standard `HTTPS_PROXY` variable, or provide drivers manually with
`-Dwebdriver.chrome.driver=/path/chromedriver` (`webdriver.gecko.driver`, `webdriver.edge.driver`).

### Safari: `Could not create a session: You must enable 'Allow remote automation'`

Run `safaridriver --enable` once (macOS asks for your password) and enable *Develop → Allow Remote Automation*.

### Scenarios are reported twice / not found

- Run through `CucumberTestSuite` (`./mvnw test`); the pom excludes direct discovery by the Cucumber engine.
- Feature files must be under `src/test/resources/features/`.
- Step classes must be under the `cucumber.glue` package (`io.github.andercmd.autto`). After renaming the package,
  update `cucumber.glue` in `junit-platform.properties`.

### `No browser is running on thread ...`

A page object was used in a scenario tagged `@nobrowser`, or outside a scenario. Remove the tag or start a browser
with `DriverManager.start()`.

### Video does not play in the report

- Open-source Chromium does not ship the H.264 codec; use Chrome/Edge/Firefox/Safari or the *Download video* link.
- Videos are only kept for failed scenarios by default (`video.mode=on_failure`).

### Alerts disappear unexpectedly

With video recording on, background screenshots could dismiss alerts, so `browser.unhandled.prompt` defaults to
`ignore`. If you changed it, set it back or disable video for those scenarios.

### Tests are flaky

- Never use `Thread.sleep`; use `BasePage` waits (`visible`, `clickable`, `waitUntil`) or Awaitility.
- Keep `timeouts.implicit=0`.
- Make scenarios independent and create their own data.
- Re-run the failures with `-Dcucumber.features=@target/autto-reports/rerun.txt` to tell flaky from broken.

### The Maven build fails although the report is fine

Failing scenarios fail the build on purpose. Use `-Dautto.ignoreFailures=true` when a pipeline step must continue
(e.g. to publish reports), and read the result from the JUnit XML.
