# Configuración

[← Volver al README](../../README.es.md) · [English](../en/configuration.md)

La configuración vive en `autto-e2e/src/test/resources/application.yml` y se enlaza al record tipado
`AuttoProperties`. IntelliJ IDEA y VS Code (Spring Boot tools) autocompletan cada clave `autto.*` con su descripción
gracias a la metadata incluida en `autto-core`.

## Precedencia

De mayor a menor:

1. Propiedades del sistema JVM — `./mvnw test -Dautto.browser.name=firefox`
2. Variables de entorno — `AUTTO_BROWSER_NAME=firefox` (relaxed binding: puntos y guiones se convierten en `_`)
3. Archivo `.env` (ignorado por git) — ver [Secretos](secretos.md)
4. `application-<perfil>.yml` de cada perfil activo (gana el último perfil)
5. `application.yml`
6. Valores por defecto de `AuttoProperties`

Un valor inválido detiene la ejecución de inmediato indicando la clave, p. ej.
`Invalid Autto configuration: ... autto.evidence.video-fps must be between 1 and 30`.

## Perfiles

```bash
./mvnw test -Dspring.profiles.active=staging            # un entorno
./mvnw test -Dspring.profiles.active=qa,ci,grid         # entorno + ajustes de CI + Selenium Grid
SPRING_PROFILES_ACTIVE=qa,docker ./mvnw test            # variable de entorno (o en .env)
```

| Perfil | Propósito |
|---|---|
| `qa` (por defecto) | URL del entorno QA |
| `staging`, `prod` | Otros entornos (`prod` desactiva videos) |
| `ci` | Navegadores headless |
| `docker` | Navegadores en contenedores Docker desechables (WebDriverManager) |
| `grid` | Selenium Grid (`docker compose up -d`) |
| `browserstack` | BrowserStack (credenciales desde `.env` / secretos del CI) |

Crea `application-local.yml` (ignorado por git) para tus ajustes personales y ejecuta con
`-Dspring.profiles.active=qa,local`.

## Referencia

Las duraciones aceptan `500ms`, `15s`, `5m`. Los enums aceptan `on-failure`, `ON_FAILURE`, `onFailure`.

### `autto` (raíz)

| Clave | Por defecto | Descripción |
|---|---|---|
| `autto.base-url` | — | URL base que usa `BasePage.open("/ruta")`. |

### `autto.browser`

| Clave | Por defecto | Descripción |
|---|---|---|
| `name` | `chrome` | `chrome`, `chromium`, `firefox`, `edge`, `safari`. |
| `version` | instalado | Versión exacta o canal (`stable`, `beta`, `dev`, `canary`); usa Selenium Manager, que puede descargarlo. |
| `headless` | `false` | Modo headless (Safari lo ignora). |
| `window-size` | `1920x1080` | `maximized` o `<ancho>x<alto>`. |
| `args` | `[]` | Argumentos extra de línea de comandos. |
| `binary` | — | Ejecutable propio (Brave, builds de Chromium, Firefox Developer Edition…). |
| `incognito` | `false` | Ventana privada / incógnito. |
| `mobile-emulation` | — | Emulación de dispositivo en Chromium, p. ej. `iPhone 14 Pro Max`. |
| `accept-insecure-certs` | `true` | Aceptar certificados autofirmados. |
| `page-load-strategy` | `normal` | `normal`, `eager`, `none`. |
| `download-dir` | `target/downloads` | Carpeta de descargas (Chromium y Firefox). |
| `unhandled-prompt` | `ignore` mientras se graba | `accept`, `dismiss`, `accept and notify`, `dismiss and notify`, `ignore`. |
| `console-logs` | `true` | Recoger mensajes de consola y errores JavaScript (WebDriver BiDi), adjuntos al fallar. |
| `prefs.<nombre>` | — | Preferencias del navegador, p. ej. `intl.accept_languages: es-ES`. |

### `autto.driver`

| Clave | Por defecto | Descripción |
|---|---|---|
| `resolution` | `webdrivermanager` | `webdrivermanager` o `selenium-manager`. |
| `fallback` | `true` | Usar Selenium Manager cuando WebDriverManager falla. |
| `docker-fallback` | `false` | Arrancar el navegador en Docker si no está instalado (requiere Docker). |
| `cache-path` | `~/.cache/selenium` | Caché de drivers de WebDriverManager. |

WebDriverManager también lee sus propias propiedades `wdm.*` / variables `WDM_*` (proxy, mirrors, timeouts), p. ej.
`-Dwdm.proxy=proxy.empresa.com:8080`.

### `autto.execution`

| Clave | Por defecto | Descripción |
|---|---|---|
| `target` | `local` | `local`, `docker`, `remote` (Grid / nube) o `appium`. |
| `remote-url` | `http://localhost:4444` | Grid o hub del proveedor. Las credenciales de la URL se ocultan en logs y reportes. |
| `platform-name` | — | SO solicitado en sesiones remotas (`Windows 11`, `macOS 15`, `linux`). |
| `capabilities` | `{}` | Cualquier capability W3C / de proveedor (ver abajo). |

```yaml
autto:
  execution:
    capabilities:
      se:recordVideo: true              # los nombres con ':' funcionan tal cual ("[se:recordVideo]" también vale)
      bstack:options:
        os: Windows
        osVersion: '"11"'               # las comillas internas lo mantienen como texto
      goog:chromeOptions:
        args: [--lang=es]
```

### `autto.docker` (target `docker`)

| Clave | Por defecto | Descripción |
|---|---|---|
| `vnc` | `false` | Expone una URL noVNC (aparece en el log) para ver el navegador. |
| `screen-resolution` | `1920x1080x24` | Pantalla virtual. |
| `shm-size` | `2g` | Memoria compartida del contenedor. |

### `autto.appium` (target `appium`)

| Clave | Por defecto | Descripción |
|---|---|---|
| `url` | `http://127.0.0.1:4723` | Servidor Appium. |
| `platform` | `android` | `android` o `ios`. |
| `app` | — | Ruta o URL del `.apk` / `.ipa` / `.app`. Omítelo para web móvil. |

### `autto.timeouts`

| Clave | Por defecto | Descripción |
|---|---|---|
| `implicit` | `0s` | Mantenla en 0: el framework usa esperas explícitas. |
| `explicit` | `15s` | Espera explícita por defecto de `BasePage`. |
| `page-load` | `60s` | Timeout de carga de página. |
| `script` | `30s` | Timeout de scripts asíncronos. |
| `polling` | `250ms` | Intervalo de sondeo de las esperas explícitas. |

### `autto.evidence`

| Clave | Por defecto | Descripción |
|---|---|---|
| `screenshot` | `on-failure` | `off`, `on-failure`, `always` (después de cada step). |
| `video` | `on-failure` | `off`, `on-failure` (siempre se graba, se conserva solo si falla), `always`. |
| `video-fps` | `3` | 1–30. |
| `video-max-duration` | `5m` | Solo se conserva la parte final de escenarios largos. |
| `video-max-width` | `1280` | Los fotogramas se reducen a este ancho. |
| `page-source` | `true` | Adjuntar el HTML de la página cuando falla un escenario. |

### `autto.report`

| Clave | Por defecto | Descripción |
|---|---|---|
| `dir` | `target/autto-reports` | Carpeta de salida (relativa al módulo). |
| `timestamped` | `false` | Una subcarpeta por ejecución. |
| `title` / `name` | Autto… | Título de la pestaña / nombre en la cabecera. |
| `theme` | `dark` | `dark` o `standard`. |
| `offline` | `true` | Copia los recursos para abrir el reporte sin internet. |
| `timeline` | `true` | Gráfico de línea de tiempo. |
| `screenshots-base64` | `false` | Incrustar las capturas en el HTML. |
| `author` | — | Autor por defecto si el escenario no tiene tag `@author:<nombre>`. |
| `show-host` | `true` | Mostrar usuario y equipo en el dashboard. |
| `info.<etiqueta>` | — | Filas extra del dashboard. |

### Cucumber (`junit-platform.properties`)

| Clave | Por defecto | Descripción |
|---|---|---|
| `cucumber.filter.tags` | `not @wip and not @ignore and not @demo-failure` | Expresión de tags. |
| `cucumber.features` | — | Sobrescribe las features (`classpath:features/login`, `@target/autto-reports/rerun.txt`). |
| `cucumber.glue` | `io.github.andercmd.autto.e2e,io.github.andercmd.autto.core.cucumber` | Steps + hooks del framework. |
| `cucumber.execution.parallel.enabled` | `false` | Escenarios en paralelo. |
| `cucumber.execution.parallel.config.fixed.parallelism` | `4` | Hilos. |

### Otros

| Clave | Por defecto | Descripción |
|---|---|---|
| `autto.dotenv.path` / `AUTTO_DOTENV_PATH` | automático | Ubicación explícita del `.env`. |
| `autto.log.level` | `INFO` | Nivel de log del framework y las pruebas (`DEBUG` traza cada interacción). |
| `autto.ignoreFailures` | `false` | Mantener el build en verde aunque fallen escenarios. |
| `checkstyle.skip` | `false` | Saltar el estándar de código (no recomendado). |
