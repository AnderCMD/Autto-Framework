# Ejecución

[← Volver al README](../../README.es.md) · [English](../en/running-tests.md)

> Windows: usa `mvnw.cmd`. PowerShell: pon los `-D` entre comillas (`"-Dautto.browser.name=firefox"`).
> Los comandos usan `-pl autto-e2e` tras un primer `./mvnw install`; usa `./mvnw install` para reconstruir todo.

## Seleccionar escenarios

```bash
./mvnw -pl autto-e2e test                                                   # el filtro por defecto excluye @wip, @ignore, @demo-failure
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@smoke"
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@regression and not @slow"
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@api"                   # solo API, sin navegador (rápido)
./mvnw -pl autto-e2e test -Dcucumber.filter.tags="@accessibility"         # auditorías axe-core
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/login
./mvnw -pl autto-e2e test -Dcucumber.features=classpath:features/login/login.feature:12
./mvnw -pl autto-e2e test -Dcucumber.filter.name="Successful login"
./mvnw -pl autto-e2e test -Dcucumber.features=@target/autto-reports/rerun.txt    # re-ejecutar los fallos
./mvnw -pl autto-core test                                                  # solo tests unitarios del framework
```

## Entornos (perfiles)

```bash
./mvnw -pl autto-e2e test -Dspring.profiles.active=staging
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,ci
```

## Navegadores

```bash
./mvnw -pl autto-e2e test -Dautto.browser.name=chrome
./mvnw -pl autto-e2e test -Dautto.browser.name=chromium
./mvnw -pl autto-e2e test -Dautto.browser.name=firefox
./mvnw -pl autto-e2e test -Dautto.browser.name=edge
./mvnw -pl autto-e2e test -Dautto.browser.name=safari                # macOS: ejecuta `safaridriver --enable` una vez
./mvnw -pl autto-e2e test -Dautto.browser.version=beta               # Selenium Manager descarga Chrome Beta
./mvnw -pl autto-e2e test -Dautto.browser.headless=true -Dautto.browser.window-size=1366x768
./mvnw -pl autto-e2e test -Dautto.browser.mobile-emulation="iPhone 14 Pro Max"
./mvnw -pl autto-e2e test -Dautto.browser.binary="/Applications/Brave Browser.app/Contents/MacOS/Brave Browser"
```

### Cómo se resuelven los drivers ("cualquier navegador, sin importar qué")

1. **WebDriverManager** detecta el navegador instalado, descarga el driver compatible y lo cachea (una vez por
   ejecución).
2. Si falla (sin conexión, URL bloqueada…), **Selenium Manager** toma el relevo automáticamente.
3. Con `autto.driver.docker-fallback=true`, un navegador **no instalado** arranca en **Docker**.
4. Con el perfil `docker` todos los navegadores corren en contenedores desechables; solo se necesita Docker.

Detrás de un proxy corporativo: `-Dwdm.proxy=proxy.empresa.com:8080` (WebDriverManager) y `HTTPS_PROXY`
(Selenium Manager).

## Ejecución en paralelo

```bash
./mvnw -pl autto-e2e test -Dcucumber.execution.parallel.enabled=true -Dcucumber.execution.parallel.config.fixed.parallelism=4
```

Cada escenario tiene su navegador, video, nodo de reporte y beans de Spring de scope escenario.

## Plataformas

### Local — Windows, macOS, Linux

Por defecto (`autto.execution.target=local`).

### Navegadores en Docker (sin instalar nada)

```bash
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,docker -Dautto.browser.name=firefox
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,docker -Dautto.docker.vnc=true      # verlo en vivo (URL en el log)
```

### Selenium Grid

```bash
docker compose up -d
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,grid -Dautto.browser.name=edge
docker compose down
```

Consola del Grid: <http://localhost:4444/ui>.

### Proveedores en la nube

El perfil `browserstack` está listo; las credenciales vienen de `.env` / secretos del CI:

```dotenv
BROWSERSTACK_USERNAME=...
BROWSERSTACK_ACCESS_KEY=...
```

```bash
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,browserstack
```

Sauce Labs / LambdaTest: copia `application-browserstack.yml` y cambia `remote-url` y las capabilities:

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

### Móvil (Appium 2+)

```yaml
# application-android.yml
autto:
  execution:
    target: appium
    capabilities:
      browserName: Chrome              # web móvil; omítelo y define autto.appium.app para apps nativas
      appium:deviceName: Pixel 8
  appium:
    platform: android
```

```bash
appium &                                # npm i -g appium && appium driver install uiautomator2
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,android
```

## Flags útiles

| Flag | Efecto |
|---|---|
| `-Dautto.ignoreFailures=true` | No falla el build si fallan escenarios (los reportes siempre se generan). |
| `-Dautto.log.level=DEBUG` | Traza cada interacción. |
| `-Dautto.evidence.video=always` | Conserva el video de todos los escenarios. |
| `-Dautto.evidence.screenshot=always` | Captura después de cada step. |
| `-Dautto.report.timestamped=true` | Una carpeta de reporte por ejecución. |
| `-Djava.version=25` | Compilar con un JDK anterior. |
