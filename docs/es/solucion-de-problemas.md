# Solución de problemas

[← Volver al README](../../README.es.md) · [English](../en/troubleshooting.md)

### `Autto targets Java 27...` (Maven Enforcer)

Tu JDK es anterior a `java.version`. Instala JDK 27 (Early Access hasta marzo de 2027) o compila con
`-Djava.version=25` (cualquier valor ≥ 21). Hazlo permanente en `.mvn/maven.config`.

### `SessionNotCreatedException: ... user data directory is already in use` / Chrome se cierra en Linux

Normalmente Chrome no puede iniciar su sandbox (ejecutando como root o dentro de un contenedor). El framework añade
`--no-sandbox --disable-dev-shm-usage` automáticamente al ejecutar como root, en CI (variable `CI`) o en Docker. En
otros casos añádelos con `-Dbrowser.args=--no-sandbox,--disable-dev-shm-usage`.

### `This version of ChromeDriver only supports Chrome version N`

Hay un driver en el `PATH` (o en `webdriver.chrome.driver`) que no coincide con el navegador. Elimínalo y deja que
Selenium Manager resuelva el correcto, o fija la versión del navegador con `-Dbrowser.version=<versión>`.

### Selenium Manager no puede descargar drivers (proxy corporativo / sin conexión)

Define la variable estándar `HTTPS_PROXY`, o proporciona los drivers manualmente con
`-Dwebdriver.chrome.driver=/ruta/chromedriver` (`webdriver.gecko.driver`, `webdriver.edge.driver`).

### Safari: `Could not create a session: You must enable 'Allow remote automation'`

Ejecuta `safaridriver --enable` una vez (macOS pide tu contraseña) y activa *Desarrollo → Permitir automatización
remota*.

### Los escenarios se reportan dos veces / no se encuentran

- Ejecuta mediante `CucumberTestSuite` (`./mvnw test`); el pom excluye el descubrimiento directo del engine de
  Cucumber.
- Los archivos `.feature` deben estar en `src/test/resources/features/`.
- Las clases de steps deben estar dentro del paquete `cucumber.glue` (`io.github.andercmd.autto`). Tras renombrar el
  paquete, actualiza `cucumber.glue` en `junit-platform.properties`.

### `No browser is running on thread ...`

Se usó un page object en un escenario con tag `@nobrowser`, o fuera de un escenario. Quita el tag o inicia un
navegador con `DriverManager.start()`.

### El video no se reproduce en el reporte

- Chromium open source no incluye el códec H.264; usa Chrome/Edge/Firefox/Safari o el enlace *Download video*.
- Por defecto los videos solo se conservan para escenarios fallidos (`video.mode=on_failure`).

### Las alertas desaparecen inesperadamente

Con la grabación de video activa, las capturas en segundo plano podrían cerrar alertas, por eso
`browser.unhandled.prompt` es `ignore` por defecto. Si lo cambiaste, restáuralo o desactiva el video en esos
escenarios.

### Pruebas inestables (flaky)

- Nunca uses `Thread.sleep`; usa las esperas de `BasePage` (`visible`, `clickable`, `waitUntil`) o Awaitility.
- Mantén `timeouts.implicit=0`.
- Haz los escenarios independientes y que creen sus propios datos.
- Re-ejecuta los fallos con `-Dcucumber.features=@target/autto-reports/rerun.txt` para distinguir flaky de roto.

### El build de Maven falla aunque el reporte esté bien

Los escenarios fallidos hacen fallar el build a propósito. Usa `-Dautto.ignoreFailures=true` cuando un paso del
pipeline deba continuar (p. ej. para publicar reportes) y obtén el resultado del XML de JUnit.
