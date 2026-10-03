# Configuración

[← Volver al README](../../README.es.md) · [English](../en/configuration.md)

## Orden de resolución

Cada valor se busca en cuatro capas; gana la última:

1. `src/test/resources/autto.properties` — valores por defecto del proyecto
2. `src/test/resources/environments/<env>.properties` — valores del entorno (`env=qa` por defecto)
3. Variables de entorno con prefijo `AUTTO_` — `browser.headless` → `AUTTO_BROWSER_HEADLESS`
4. Propiedades del sistema de la JVM — `./mvnw test -Dbrowser.headless=true`

Los valores vacíos se ignoran, así un `-Dbrowser=` vacío nunca borra un valor configurado. Los valores admiten
`${placeholders}` (ver [Placeholders](#placeholders)).

```bash
# Seleccionar otro archivo de entorno
./mvnw test -Denv=staging
AUTTO_ENV=staging ./mvnw test
```

Crea `environments/local.properties` (ignorado por git) para tus ajustes personales y ejecuta con `-Denv=local`.

## Referencia

### General

| Clave | Por defecto | Descripción |
|---|---|---|
| `env` | `qa` | Archivo de entorno a cargar desde `environments/`. |
| `base.url` | — | URL base que usa `BasePage.open("/ruta")`. |

### Navegador

| Clave | Por defecto | Descripción |
|---|---|---|
| `browser` | `chrome` | `chrome`, `firefox`, `edge`, `safari`. |
| `browser.version` | instalado | Versión exacta o canal (`stable`, `beta`, `dev`, `canary`). Selenium Manager la descarga si hace falta. |
| `browser.headless` | `false` | Modo headless (Safari lo ignora). |
| `browser.window.size` | `1920x1080` | `maximized` o `<ancho>x<alto>`. |
| `browser.args` | — | Argumentos extra de línea de comandos, separados por comas. |
| `browser.binary` | — | Binario propio del navegador (Chromium, Brave, Firefox Developer Edition…). |
| `browser.incognito` | `false` | Ventana privada/incógnito. |
| `browser.mobile.emulation` | — | Emulación de dispositivo en Chromium, p. ej. `iPhone 14 Pro Max`, `Pixel 7`. |
| `browser.accept.insecure.certs` | `true` | Aceptar certificados autofirmados. |
| `browser.page.load.strategy` | `normal` | `normal`, `eager`, `none`. |
| `browser.download.dir` | `target/downloads` | Carpeta de descargas (Chromium y Firefox). |
| `browser.unhandled.prompt` | `ignore` si hay video | `accept`, `dismiss`, `accept and notify`, `dismiss and notify`, `ignore`. |
| `browser.console.logs` | `true` | Recoger mensajes de consola y errores JS vía WebDriver BiDi y adjuntarlos si falla. |
| `browser.prefs.<nombre>` | — | Preferencias del navegador (Chromium `prefs` / perfil de Firefox), p. ej. `browser.prefs.intl.accept_languages=es-ES`. |

### Destino de ejecución

| Clave | Por defecto | Descripción |
|---|---|---|
| `execution.target` | `local` | `local`, `remote` (Grid / nube) o `appium`. |
| `remote.url` | `http://localhost:4444` | URL del Selenium Grid o del hub del proveedor. Las credenciales de la URL se ocultan en logs. |
| `platform.name` | — | SO solicitado en sesiones remotas (`Windows 11`, `macOS 15`, `linux`). |
| `capabilities.<nombre>` | — | Cualquier capability W3C / de proveedor, ver abajo. |
| `appium.url` | `http://127.0.0.1:4723` | Servidor Appium. |
| `appium.platform` | `android` | `android` o `ios`. |
| `appium.app` | — | Ruta o URL del `.apk` / `.ipa` / `.app`. Omítelo para web móvil. |

#### Capabilities

Las claves planas se convierten en JSON anidado. Los valores se convierten a booleanos/números salvo que vayan entre
comillas; `[a,b]` es una lista.

```properties
capabilities.se:recordVideo=true
capabilities.bstack:options.os=Windows
capabilities.bstack:options.osVersion="11"
capabilities.goog:chromeOptions.args=[--lang=es]
capabilities.appium:deviceName=Pixel 8
```

#### Placeholders

Cualquier valor puede referenciar otra clave o una variable de entorno con `${NOMBRE}` o `${NOMBRE:por_defecto}`.
Así las credenciales nunca entran al repositorio:

```properties
remote.url=https://${BROWSERSTACK_USERNAME}:${BROWSERSTACK_ACCESS_KEY}@hub-cloud.browserstack.com/wd/hub
capabilities.bstack:options.buildName=${GITHUB_RUN_ID:local-build}
```

Un placeholder sin valor por defecto que no puede resolverse falla con un mensaje claro al leer la clave.

### Timeouts (segundos)

| Clave | Por defecto | Descripción |
|---|---|---|
| `timeouts.implicit` | `0` | Espera implícita. Mantenla en 0; el framework usa esperas explícitas. |
| `timeouts.explicit` | `15` | Espera explícita por defecto de `BasePage`. |
| `timeouts.page.load` | `60` | Timeout de carga de página. |
| `timeouts.script` | `30` | Timeout de scripts asíncronos. |
| `timeouts.polling.ms` | `250` | Intervalo de sondeo de las esperas explícitas (milisegundos). |

### Evidencias

| Clave | Por defecto | Descripción |
|---|---|---|
| `screenshot.mode` | `on_failure` | `off`, `on_failure`, `always` (después de cada step). |
| `video.mode` | `on_failure` | `off`, `on_failure` (se graba siempre, se conserva solo si falla), `always`. |
| `video.fps` | `3` | Fotogramas por segundo (1–30). Más fps implica más llamadas WebDriver. |
| `video.max.seconds` | `300` | Solo se conservan los últimos N segundos del escenario. |
| `video.max.width` | `1280` | Los fotogramas se reducen a este ancho. |
| `evidence.page.source` | `true` | Adjuntar el HTML de la página cuando falla un escenario. |

### Reporte

| Clave | Por defecto | Descripción |
|---|---|---|
| `report.dir` | `target/autto-reports` | Carpeta de salida del reporte Extent y las evidencias. |
| `report.timestamped` | `false` | Una subcarpeta por ejecución (`yyyyMMdd-HHmmss`) para conservar historial. |
| `report.title` | `Autto · Test Automation Report` | Título de la pestaña del navegador. |
| `report.name` | `Autto Framework · Execution report` | Nombre mostrado en la cabecera. |
| `report.theme` | `dark` | `dark` o `standard`. |
| `report.offline` | `true` | Copia los recursos del reporte para abrirlo sin internet. |
| `report.timeline` | `true` | Gráfico de línea de tiempo en el dashboard. |
| `report.screenshots.base64` | `false` | Incrusta las capturas en el HTML (archivo único portable, más pesado). |
| `report.author` | — | Autor por defecto cuando un escenario no tiene tag `@author:<nombre>`. |
| `report.show.host` | `true` | Mostrar usuario y nombre del equipo en el dashboard. |
| `report.info.<etiqueta>` | — | Filas extra para la tabla de entorno del dashboard. |

### Cucumber (`junit-platform.properties`)

| Clave | Por defecto | Descripción |
|---|---|---|
| `cucumber.filter.tags` | `not @wip and not @ignore and not @demo-failure` | Expresión de tags. |
| `cucumber.features` | — | Sobrescribe las features seleccionadas, p. ej. `classpath:features/login`. |
| `cucumber.glue` | `io.github.andercmd.autto` | Paquetes donde se buscan steps y hooks. |
| `cucumber.plugin` | Extent + HTML + JSON + JUnit + NDJSON + rerun | Plugins de reporte. |
| `cucumber.execution.parallel.enabled` | `false` | Ejecutar escenarios en paralelo. |
| `cucumber.execution.parallel.config.fixed.parallelism` | `4` | Hilos en paralelo. |

### Logging

| Clave | Por defecto | Descripción |
|---|---|---|
| `autto.log.level` | `INFO` | Nivel de log del framework y de las clases de prueba (`DEBUG` muestra cada clic/escritura). |
