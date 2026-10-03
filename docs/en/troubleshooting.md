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

### `Could not start a new session` / `SessionNotCreatedException` on Grid or cloud

The hub was busy or the vendor queue was full. Autto already retries `autto.driver.start-retries` times (default 1)
with exponential back-off; raise it for shared Grids (`-Dautto.driver.start-retries=3`) and check the hub capacity
(`max-sessions`) and your vendor's parallel limit.

### `JdkWebSocket initial request execution error (uri: ws://172.x.x.x:4444/session/.../se/bidi)` on Grid

The Grid nodes advertise WebSocket URLs (browser console logs through WebDriver BiDi) with an address the tests
cannot reach. Set the public Grid URL on the nodes: `SE_NODE_GRID_URL=http://<host-reachable-by-tests>:4444` (the
included `docker-compose.yml` uses `http://localhost:4444`). The scenario still runs; only console logs are lost.

### API requests time out or fail with SSL errors

Increase `autto.api.read-timeout` / `connect-timeout`. For test environments with self-signed certificates set
`autto.api.relaxed-https=true` (never against production). Behind a proxy pass the standard JVM settings:
`-Dhttps.proxyHost=proxy.company.com -Dhttps.proxyPort=8080`.

### Flaky tests

No `Thread.sleep` (blocked by Checkstyle), keep `autto.timeouts.implicit=0s`, make scenarios independent and
re-run failures with `-Dcucumber.features=@target/autto-reports/rerun.txt` to tell flaky from broken.
