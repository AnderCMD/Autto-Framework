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

### `JdkWebSocket initial request execution error (uri: ws://172.x.x.x:4444/session/.../se/bidi)` en Grid

Los nodos del Grid anuncian URLs de WebSocket (logs de consola del navegador vía WebDriver BiDi) con una dirección que
las pruebas no alcanzan. Define la URL pública del Grid en los nodos: `SE_NODE_GRID_URL=http://<host-accesible>:4444`
(el `docker-compose.yml` incluido usa `http://localhost:4444`). El escenario sigue ejecutándose; solo se pierden los
logs de consola.

### Las peticiones de API agotan el tiempo o fallan con errores SSL

Aumenta `autto.api.read-timeout` / `connect-timeout`. En entornos de prueba con certificados autofirmados usa
`autto.api.relaxed-https=true` (nunca contra producción). Detrás de un proxy pasa los ajustes estándar de la JVM:
`-Dhttps.proxyHost=proxy.company.com -Dhttps.proxyPort=8080`.

### Pruebas inestables (flaky)

Nada de `Thread.sleep` (lo bloquea Checkstyle), mantén `autto.timeouts.implicit=0s`, haz los escenarios
independientes y usa `./scripts/run-with-rerun.sh`: los escenarios fallidos corren una vez más y los que pasan en el segundo intento se
listan como flaky (`recovered_on_rerun` en `metrics.json`, categoría `rerun` en el reporte) en lugar de esconderse.
`BasePage` ya reintenta los elementos obsoletos; `autto.scenario.timeout` detiene los escenarios colgados. Ver
[Pruebas avanzadas](pruebas-avanzadas.md).

### `This version of ChromeDriver only supports Chrome version N`

El navegador se actualizó después de guardar el driver en caché. Autto lo detecta, refresca la resolución y reintenta
una vez. Si persiste, borra la caché (`rm -rf ~/.cache/selenium`) o usa `-Dautto.driver.resolution=selenium-manager`.

### La simulación de red expira o informa `Invalid InterceptionId`

`NetworkMock` depende de la intercepción de peticiones de WebDriver BiDi, poco fiable en algunas versiones de Chromium
(visto en Chrome 150). Mantén `autto.browser.console-logs: true`, prueba otra versión del navegador o Firefox y trata la
función como experimental hasta que pase en la tuya.

### La regresión visual difiere entre máquinas

Las líneas base dependen del navegador, versión, sistema operativo y fuentes. Créalas y compáralas en el mismo
entorno, preferiblemente el target Docker, y sube `autto.visual.tolerance` / ignora zonas dinámicas solo si hace
falta.
