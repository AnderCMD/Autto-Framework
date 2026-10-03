# Troubleshooting

[← Back to README](../../README.md) · [Español](../es/solucion-de-problemas.md)

### `Autto targets Java 27...` (Maven Enforcer)

Your JDK is older than `java.version`. Install JDK 27 (Early Access until March 2027) or add `-Djava.version=25`
(any value ≥ 21), permanently in `.mvn/maven.config`.

### `The password of test user 'standard' is not available`

The secret is missing: `cp .env.example .env` and fill it in, or define the environment variable / CI secret
(`SAUCE_PASSWORD`). The log line `Autto configuration loaded · profiles [...] · .env <path>` shows which `.env` was
used.

### `Invalid Autto configuration: ...`

A value in `application*.yml`, `.env`, an `AUTTO_*` variable or a `-D` property has the wrong type or range. The
message names the key.

### `WebDriverManager could not resolve the driver ... Falling back to Selenium Manager`

WebDriverManager could not reach its metadata URLs (proxy, firewall, offline). The run continues with Selenium
Manager. Configure the proxy (`-Dwdm.proxy=host:port`) or a mirror, or set
`autto.driver.resolution=selenium-manager` to skip WebDriverManager.

### The browser is not installed

- Set `autto.driver.docker-fallback=true` (Docker required), or
- use the `docker` profile, or
- request a version (`-Dautto.browser.version=stable`): Selenium Manager downloads Chrome/Firefox.

### Chrome crashes on Linux / `user data directory is already in use`

Chrome cannot start its sandbox (root or container). The framework adds `--no-sandbox --disable-dev-shm-usage`
when running as root, in CI or in Docker; otherwise add them to `autto.browser.args`.

### Safari: `You must enable 'Allow remote automation'`

Run `safaridriver --enable` once and enable *Develop → Allow Remote Automation*.

### `No qualifying bean of type ...` / `Could not find @CucumberContextConfiguration`

- Pages must be annotated with `@PageObject` (or `@Component`) and live under the package of
  `E2eTestApplication`.
- Exactly one class annotated with `@CucumberContextConfiguration` must be in the glue path.
- `cucumber.glue` must contain your package and `io.github.andercmd.autto.core.cucumber`.

### `No browser is running on thread ...`

A page object was used in a `@nobrowser` scenario or outside a scenario. Remove the tag or call
`DriverManager.start()`.

### Checkstyle fails the build

Read the reported rule: line length (120), unused imports, `Thread.sleep`, `System.out`, implicit waits... Fix the
code; `-Dcheckstyle.skip` exists only for emergencies.

### Video does not play

Open-source Chromium lacks the H.264 codec: use Chrome/Edge/Firefox/Safari or *Download video*. Videos are kept only
for failed scenarios by default.

### Flaky tests

No `Thread.sleep` (blocked by Checkstyle), keep `autto.timeouts.implicit=0s`, make scenarios independent and
re-run failures with `-Dcucumber.features=@target/autto-reports/rerun.txt` to tell flaky from broken.
