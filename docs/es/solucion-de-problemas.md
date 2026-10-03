# Solución de problemas

[← Volver al README](../../README.es.md) · [English](../en/troubleshooting.md)

### `Autto targets Java 27...` (Maven Enforcer)

Tu JDK es anterior a `java.version`. Instala JDK 27 (Early Access hasta marzo de 2027) o añade `-Djava.version=25`
(cualquier valor ≥ 21), de forma permanente en `.mvn/maven.config`.

### `The password of test user 'standard' is not available`

Falta el secreto: `cp .env.example .env` y complétalo, o define la variable de entorno / secreto del CI
(`SAUCE_PASSWORD`). La línea de log `Autto configuration loaded · profiles [...] · .env <ruta>` indica qué `.env` se
usó.

### `Invalid Autto configuration: ...`

Un valor de `application*.yml`, `.env`, una variable `AUTTO_*` o una propiedad `-D` tiene tipo o rango incorrecto. El
mensaje indica la clave.

### `WebDriverManager could not resolve the driver ... Falling back to Selenium Manager`

WebDriverManager no pudo acceder a sus URLs de metadatos (proxy, firewall, sin conexión). La ejecución continúa con
Selenium Manager. Configura el proxy (`-Dwdm.proxy=host:puerto`) o un mirror, o usa
`autto.driver.resolution=selenium-manager` para omitir WebDriverManager.

### El navegador no está instalado

- Activa `autto.driver.docker-fallback=true` (requiere Docker), o
- usa el perfil `docker`, o
- pide una versión (`-Dautto.browser.version=stable`): Selenium Manager descarga Chrome/Firefox.

### Chrome se cierra en Linux / `user data directory is already in use`

Chrome no puede iniciar su sandbox (root o contenedor). El framework añade `--no-sandbox --disable-dev-shm-usage`
al ejecutar como root, en CI o en Docker; en otros casos añádelos a `autto.browser.args`.

### Safari: `You must enable 'Allow remote automation'`

Ejecuta `safaridriver --enable` una vez y activa *Desarrollo → Permitir automatización remota*.

### `No qualifying bean of type ...` / `Could not find @CucumberContextConfiguration`

- Las páginas deben llevar `@PageObject` (o `@Component`) y estar bajo el paquete de `E2eTestApplication`.
- Debe haber exactamente una clase con `@CucumberContextConfiguration` en el glue.
- `cucumber.glue` debe contener tu paquete e `io.github.andercmd.autto.core.cucumber`.

### `No browser is running on thread ...`

Se usó un page object en un escenario `@nobrowser` o fuera de un escenario. Quita el tag o llama a
`DriverManager.start()`.

### Checkstyle hace fallar el build

Lee la regla indicada: longitud de línea (120), imports sin usar, `Thread.sleep`, `System.out`, esperas
implícitas... Corrige el código; `-Dcheckstyle.skip` existe solo para emergencias.

### El video no se reproduce

Chromium open source no incluye el códec H.264: usa Chrome/Edge/Firefox/Safari o *Download video*. Por defecto los
videos solo se conservan para escenarios fallidos.

### `Could not start a new session` / `SessionNotCreatedException` en Grid o en la nube

El hub estaba ocupado o la cola del proveedor llena. Autto ya reintenta `autto.driver.start-retries` veces (1 por
defecto) con back-off exponencial; súbelo en Grids compartidos (`-Dautto.driver.start-retries=3`) y revisa la
capacidad del hub (`max-sessions`) y el límite de paralelismo de tu proveedor.

### Las peticiones de API agotan el tiempo o fallan con errores SSL

Aumenta `autto.api.read-timeout` / `connect-timeout`. En entornos de prueba con certificados autofirmados usa
`autto.api.relaxed-https=true` (nunca contra producción). Detrás de un proxy pasa los ajustes estándar de la JVM:
`-Dhttps.proxyHost=proxy.company.com -Dhttps.proxyPort=8080`.

### Pruebas inestables (flaky)

Nada de `Thread.sleep` (lo bloquea Checkstyle), mantén `autto.timeouts.implicit=0s`, haz los escenarios
independientes y re-ejecuta los fallos con `-Dcucumber.features=@target/autto-reports/rerun.txt` para distinguir
flaky de roto.
