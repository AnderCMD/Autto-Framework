# Pruebas avanzadas y robustez

[← Volver al README](../../README.es.md) · [English](../en/advanced-testing.md)

Todo lo de esta guía es opcional: usa solo lo que tu suite necesite. Las clases viven en `autto-core`; cada ajuste
tiene un valor seguro por defecto y está documentado en [Configuración](configuracion.md).

## Ejecuciones estables

### Los elementos obsoletos se reintentan solos

Las aplicaciones de una sola página se redibujan constantemente, así que `StaleElementReferenceException` es la causa
más común de pruebas UI inestables. `BasePage` reintenta la interacción completa (localizar → actuar) hasta 3 veces en
`type`, `text`, `texts`, `attribute`, `select*`, `jsClick`, `doubleClick`, `pressKeys` y `hover`; `click` y las
esperas ya lo hacían. Envuelve tus propias interacciones con `retryStale(() -> ...)`.

### Timeout por escenario

`autto.scenario.timeout` (por defecto `10m`, `0` lo desactiva) aborta un escenario colgado: se cierra su navegador, se
interrumpe el hilo y el escenario falla con un mensaje en el reporte. Un escenario colgado ya no bloquea una
ejecución en paralelo.

### Re-ejecutar fallidos y marcar los inestables (flaky)

```bash
./scripts/run-with-rerun.sh -Dcucumber.filter.tags=@smoke -Dautto.browser.headless=true
```

La pasada 1 corre normal. Si hay fallos, **solo esos** escenarios corren otra vez en
`autto-e2e/target/autto-reports-rerun` (con la categoría `rerun`). Un escenario que pasa en el segundo intento es
*flaky*: no rompe el build, pero se cuenta en `metrics.json` (`recovered_on_rerun`) y se ve en el reporte de la
re-ejecución para poder corregirlo. Uno que falla dos veces rompe el build. Los workflows de CI usan este script.

### Datos de prueba únicos

Los escenarios en paralelo sobre un ambiente compartido chocan en correos, usuarios y referencias. `Unique` genera
valores que nunca se repiten entre hilos, escenarios y ejecuciones:

```java
String email = Unique.email();          // autto.k3j9x2.1@example.test
String user  = Unique.of("cliente");    // cliente-k3j9x2-2
```

Usa `TestData.faker()` (Datafaker) para nombres y direcciones realistas y `Unique` para todo lo que deba ser único.

### Driver y navegador desincronizados

Los navegadores se actualizan solos y entonces un driver en caché se niega a iniciar (`This version of ChromeDriver
only supports Chrome version 150`). Autto lo detecta, olvida la resolución en caché, resuelve el driver otra vez y
reintenta una vez, así que una actualización de Chrome durante la noche no rompe la primera ejecución de la mañana.

## Etiquetas de entorno

Las etiquetas cambian el navegador de un escenario sin código:

| Etiqueta | Efecto |
|---|---|
| `@viewport:390x844` | Cambia el tamaño de la ventana (móvil, tablet, escritorio) |
| `@slow-network` | Limita la red, 400 kbps y 400 ms de latencia (Chromium) |
| `@offline` | Corta la red (Chromium) |
| `@nobrowser` | Sin navegador (API, datos y lógica) |

Para otros idiomas o regiones define `autto.browser.prefs.intl.accept_languages` por perfil (`application-es.yml`).

## Datos y servicios

### Base de datos

`Database` es un asistente JDBC por escenario: añade el driver de tu base de datos al proyecto de pruebas y define
`autto.db.url`, `autto.db.username`, `autto.db.password` (enmascarada en logs y reportes).

```java
public PedidoSteps(Database db) { this.db = db; }

db.update("insert into users(email, status) values (?, ?)", email, "NEW");
db.cleanup("delete from users where email = ?", email);       // corre al terminar el escenario, aunque falle
assertThat(db.scalar("select status from orders where ref = ?", ref)).contains("PAID");
```

Pasa siempre los valores con `?`; nunca los concatenes en el SQL.

### Correo y códigos de un solo uso

```bash
docker compose --profile mail up -d       # Mailpit: SMTP localhost:1025, UI y API http://localhost:8025
```

Apunta la aplicación bajo prueba al puerto SMTP y lee lo que envió:

```java
Mailbox.Mail mail = mailbox.waitFor(email, "Restablece tu contraseña");   // espera hasta autto.mail.timeout
String codigo = mail.find("código es (\\d{6})").orElseThrow();
```

Para doble factor con app autenticadora guarda el secreto Base32 de la cuenta de prueba en `.env` y usa
`Totp.now(secreto)`.

### Pruebas de contrato

Guarda archivos JSON Schema en `src/test/resources/contracts/` (expórtalos de tu documento OpenAPI) y verifica las
respuestas:

```java
Response response = api.request().get("/orders/42");
ApiContract.assertMatches(response, "order.json");
```

Cada petición de la API lleva además la cabecera `X-Correlation-Id`, el mismo id que aparece en cada línea de log del
escenario (`%X{correlationId}`), para rastrear un escenario fallido en los logs de la aplicación bajo prueba.

## Calidad de la página

### Presupuestos de rendimiento web

```java
WebPerformance.measure().assertWithinBudget();
```

Lee Navigation y Paint Timing (TTFB, FCP, LCP, CLS, carga) de la página actual, añade una tabla al reporte y falla
listando **todas** las métricas que superan su presupuesto (`autto.performance.*`; los valores por defecto son los
umbrales "buenos" de Core Web Vitals, relájalos por ambiente). LCP y CLS solo existen en Chromium; los presupuestos de
métricas que el navegador no reporta se omiten.

### Regresión visual

```java
VisualRegression.assertMatches("login-page");
VisualRegression.assertMatches("inventory", new Rectangle(0, 0, 1920, 120));   // zona ignorada
```

1. Primera ejecución: `-Dautto.visual.update=true` escribe la línea base en `autto.visual.baseline-dir`; revísala y
   súbela al repositorio.
2. Las siguientes ejecuciones comparan píxeles. Antes se *estabiliza* la página (fuentes cargadas y dos capturas
   consecutivas idénticas) para que animaciones y cambios de fuente no generen falsas diferencias.
3. Un fallo escribe `<nombre>-actual.png` y un `<nombre>-diff.png` en rojo junto al reporte y embebe el diff en él.

Se aceptan diferencias menores a `autto.visual.pixel-threshold` por canal y hasta `autto.visual.tolerance` de píxeles
distintos. Las líneas base dependen del navegador, versión, sistema operativo y fuentes: créalas y compáralas en el
mismo entorno (el target Docker es el más reproducible).

### Simulación de red

```java
network.stub("/api/cart", 500, "{\"error\":\"boom\"}");    // la UI debe mostrar un banner de error
network.block("googletagmanager.com");                      // los terceros nunca ralentizan una prueba
network.delay("/api/products", Duration.ofSeconds(3));      // indicadores de carga
```

Chrome y Edge usan el protocolo DevTools (`NetworkInterceptor` de Selenium, que necesita los bindings DevTools de la
versión del navegador: Selenium incluye los más recientes). Firefox, y las versiones de Chromium sin bindings
coincidentes, recurren a WebDriver BiDi (`autto.browser.console-logs: true`, el valor por defecto). `delay` reenvía la
petición real tras la pausa. No se soporta con el target de ejecución `docker` (el socket DevTools del contenedor no es alcanzable desde el host: falla de inmediato con un mensaje claro); usa el target local o un Selenium Grid. Verificado en Chrome 154 (local), Chrome, Edge y Firefox en Selenium Grid, y Firefox en Docker (BiDi): respuestas simuladas, hosts bloqueados y peticiones retrasadas
(ver `features/quality`). La intercepción BiDi en Chrome 150 falló con *Invalid InterceptionId*, por eso DevTools va
primero.

### Accesibilidad

Ver [Escribir pruebas](escribir-pruebas.md) (`Accessibility.scan()`).

## Observar la ejecución

| Salida | Dónde | Uso |
|---|---|---|
| `metrics.json` | junto al reporte | Escenarios, pasados, fallidos, flaky, reintentos de arranque, duraciones |
| `metrics.prom` | junto al reporte | Formato texto de Prometheus (*textfile collector* de node_exporter) |
| Notificación | Slack, Microsoft Teams o un webhook JSON genérico | Resumen con enlace a la ejecución, opcionalmente solo si falla |
| Logs JSON | `-Dlogback.configurationFile=autto/logback-json.xml` | Un objeto JSON por línea con escenario y correlation id para Datadog, ELK, Loki |

```yaml
autto:
  notifications:
    webhook-url: ${SLACK_WEBHOOK_URL}   # un secreto: .env en local, secreto del CI en pipelines
    type: slack                          # slack | teams | generic
    only-on-failure: true
    report-url: ${REPORT_URL:}
```

Registra el plugin una vez en `junit-platform.properties`:
`io.github.andercmd.autto.core.observability.RunSummaryPlugin`.

### Allure y herramientas de gestión de pruebas

Añade `io.github.andercmd.autto.core.report.AllureResultsPlugin` a `cucumber.plugin` y escribe los resultados de Allure
(`target/allure-results`, `-Dautto.allure.results=...` para cambiarlo) con pasos, tags como etiquetas, mensajes y
trazas de fallo y todos los adjuntos (capturas, video, código de la página). Se ven con
`allure serve target/allure-results`. Es un escritor propio de Autto porque el plugin oficial `allure-cucumber7-jvm`
no funciona con la API de mensajes de Cucumber 8 que se usa aquí (`NoSuchMethodError`).

Xray, Zephyr: importa `target/autto-reports/cucumber/cucumber.json`. TestRail, Azure DevOps: `cucumber-junit.xml`.

## Aplicaciones móviles nativas

Mismas features, hooks, evidencias y reporte mediante Appium: usa el perfil `appium` (`application-appium.yml`:
emulador Android, app Ajustes del sistema) y la feature de demostración `@mobile`; después reemplaza las capabilities
por las de tu app. Los screen objects extienden `BasePage` y usan locators `AppiumBy`.

## Revisar un entorno

```bash
./scripts/doctor.sh
```

Verifica el JDK, la configuración, el `.env`, que `autto.base-url` responda, la carpeta de reportes, los navegadores
instalados y Docker, e indica qué falta. Ejecútalo primero en una máquina o runner de CI nuevos.

## Empezar un proyecto nuevo

```bash
./scripts/new-project.sh ../checkout-e2e com.acme checkout-e2e           # motor desde JitPack
./scripts/new-project.sh ../checkout-e2e com.acme checkout-e2e --local   # motor desde ./mvnw install
```

Crea un proyecto independiente (pom, suite, configuración de Spring, una primera feature, logging, `.env.example`,
Maven wrapper) que compila y pasa contra un navegador real desde el primer momento. El repositorio incluye además
`.devcontainer/` (JDK 21, Chromium, acceso a Docker) para empezar con un clic en VS Code o Codespaces.

## Controles de calidad y cadena de suministro

| Comando | Qué hace |
|---|---|
| `./mvnw -Pcoverage -pl autto-core verify` | Reporte JaCoCo y cobertura mínima de líneas (`coverage.minimum`, hoy 50 %; súbela al crecer) |
| `./mvnw -Pquality verify -DskipTests` | SpotBugs (nulos, recursos sin cerrar...), JDK 21 o 25 |
| `./mvnw -Psbom package` | SBOM CycloneDX en `target/bom.json` |
| `./mvnw -Prelease deploy` | Javadoc, firmas GPG y publicación en Maven Central |

El CI fija cada GitHub Action a un SHA de commit (Dependabot los mantiene al día), analiza las dependencias con OSV
cada noche y con dependency-review en los pull requests. Subir un tag `vX.Y.Z` ejecuta el workflow **Release**: build,
pruebas, SBOM, atestación de procedencia, firmas Sigstore sin llaves, release de GitHub y, si existen los secretos
`MAVEN_CENTRAL_*` / `GPG_*`, Maven Central.

Verificar un release descargado:

```bash
gh attestation verify autto-core-1.2.0.jar --repo AnderCMD/Autto-Framework
cosign verify-blob --bundle autto-core-1.2.0.jar.sigstore.json \
  --certificate-identity-regexp 'https://github.com/AnderCMD/Autto-Framework/.*' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com autto-core-1.2.0.jar
```
