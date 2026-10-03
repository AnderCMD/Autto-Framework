# Ejecución

[← Volver al README](../../README.es.md) · [English](../en/running-tests.md)

> En Windows usa `mvnw.cmd` en lugar de `./mvnw`. En PowerShell pon los argumentos `-D` entre comillas:
> `"-Dbrowser=firefox"`.

## Seleccionar escenarios

```bash
./mvnw test                                                   # todo (el filtro por defecto excluye @wip, @ignore, @demo-failure)
./mvnw test -Dcucumber.filter.tags="@smoke"
./mvnw test -Dcucumber.filter.tags="@regression and not @slow"
./mvnw test -Dcucumber.features=classpath:features/login      # una carpeta
./mvnw test -Dcucumber.features=classpath:features/login/login.feature:12   # un escenario (línea)
./mvnw test -Dcucumber.filter.name="Successful login"         # por nombre (coincidencia parcial)
./mvnw -Punit test                                            # solo los tests unitarios del framework
```

Re-ejecutar solo los fallos de la ejecución anterior:

```bash
./mvnw test -Dcucumber.features=@target/autto-reports/rerun.txt
```

## Navegadores

```bash
./mvnw test -Dbrowser=chrome
./mvnw test -Dbrowser=firefox
./mvnw test -Dbrowser=edge
./mvnw test -Dbrowser=safari          # macOS, ejecuta `safaridriver --enable` una vez
./mvnw test -Dbrowser=chrome -Dbrowser.version=beta
./mvnw test -Dbrowser.headless=true -Dbrowser.window.size=1366x768
./mvnw test -Dbrowser.mobile.emulation="iPhone 14 Pro Max"
```

Selenium Manager resuelve los drivers. Si falta el navegador (o se pide una versión concreta), Selenium Manager
puede descargar Chrome o Firefox en `~/.cache/selenium`.

## Ejecución en paralelo

Cada escenario tiene su propio navegador, grabador de video, nodo de reporte y `ScenarioContext`.

```bash
./mvnw test -Dcucumber.execution.parallel.enabled=true -Dcucumber.execution.parallel.config.fixed.parallelism=4
```

Los escenarios que no deben ejecutarse a la vez se pueden serializar con recursos exclusivos, p. ej.
`cucumber.execution.exclusive-resources.<tag>.read-write=<recurso>` en `junit-platform.properties`.

## Plataformas

### Local (Windows, macOS, Linux)

Por defecto (`execution.target=local`). Solo necesitas el navegador instalado.

### Selenium Grid con Docker

```bash
docker compose up -d                       # hub + nodos Chrome, Firefox y Edge
./mvnw test -Dexecution.target=remote -Dremote.url=http://localhost:4444 -Dbrowser=firefox
docker compose down
```

Mira las sesiones en vivo en <http://localhost:4444/ui> (contraseña VNC `secret`).

### Proveedores en la nube

Cualquier proveedor compatible con W3C funciona con `execution.target=remote`, la URL del hub y sus capabilities.

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

Pon esas líneas en un archivo de entorno (p. ej. `environments/browserstack.properties`) y ejecuta con
`-Denv=browserstack`.

### Móvil (Appium 2+)

```bash
npm i -g appium && appium driver install uiautomator2   # Android
appium                                                   # inicia el servidor
```

Web móvil (Chrome en Android):

```properties
execution.target=appium
appium.platform=android
capabilities.browserName=Chrome
capabilities.appium:deviceName=Pixel 8
```

App nativa:

```properties
execution.target=appium
appium.platform=ios
appium.app=/ruta/a/MyApp.app
capabilities.appium:deviceName=iPhone 16
capabilities.appium:platformVersion="18.0"
```

El `AndroidDriver` / `IOSDriver` que devuelve `DriverManager.driver()` puede convertirse (cast) cuando necesites
comandos específicos de móvil. Capturas, videos y reportes funcionan igual.

## Flags útiles

| Flag | Efecto |
|---|---|
| `-Dautto.ignoreFailures=true` | No falla el build de Maven cuando fallan escenarios (los reportes siempre se generan). |
| `-Dautto.log.level=DEBUG` | Registra cada clic, escritura y espera. |
| `-Dvideo.mode=always` | Conserva el video de todos los escenarios. |
| `-Dscreenshot.mode=always` | Captura después de cada step. |
| `-Dreport.timestamped=true` | Conserva una carpeta de reporte por ejecución. |
