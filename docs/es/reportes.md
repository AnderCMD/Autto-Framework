# Reportes y evidencias

[← Volver al README](../../README.es.md) · [English](../en/reporting.md)

## Salidas

Después de cada ejecución `autto-e2e/target/autto-reports/` contiene (carpeta configurable con `autto.report.dir`):

```
target/autto-reports/
├── index.html              Reporte Extent Spark — abre este
├── failed.html             Solo escenarios fallidos / con advertencias
├── extent.json             Datos crudos de Extent (archivo, dashboards propios)
├── spark/                  Recursos del reporte (modo offline)
├── screenshots/*.png
├── videos/*.mp4
├── attachments/*           Código fuente de páginas, CSV, PDF y cualquier otro adjunto
├── logs/autto.log          Log de ejecución con hilo y nombre del escenario
├── rerun.txt               Escenarios fallidos (re-ejecuta con -Dcucumber.features=@target/autto-reports/rerun.txt)
└── cucumber/
    ├── cucumber.html       Reporte nativo de Cucumber (archivo único)
    ├── cucumber.json       Para Jenkins Cucumber Reports, Xray, Zephyr…
    ├── cucumber-junit.xml  Para la pestaña de tests de cualquier CI
    └── cucumber.ndjson     Cucumber Messages
```

La carpeta es autocontenida: comprímela o publícala como artefacto del CI y se abre en cualquier lugar, incluso sin
conexión.

## El reporte Extent

| Vista | Qué obtienes |
|---|---|
| **Dashboard** | Inicio/fin, gráficos de features/escenarios/steps, línea de tiempo, tags, dispositivos, tabla de entorno. |
| **Tests** | Feature → Escenario (o Scenario Outline → ejemplos) → steps Gherkin con keyword, estado, duración, tablas, doc strings, logs, capturas, videos y stack traces. |
| **Tags** | Estadísticas por tag de Cucumber (`@smoke`, `@checkout`…). |
| **Devices** | Estadísticas por navegador/dispositivo (`chrome-141.0-windows`), útil en ejecuciones cross-browser. |
| **Authors** | Estadísticas por tag `@author:<nombre>`. |
| **Exceptions** | Cada tipo de excepción con los escenarios que la lanzaron. |

### Dónde aparece cada evidencia

| Evidencia | Ubicación en el reporte |
|---|---|
| Captura al fallar (`autto.evidence.screenshot=on-failure`) | Debajo del step que falla |
| Captura después de cada step (`autto.evidence.screenshot=always`) | Debajo de cada step |
| Llamadas `Report.*` y `scenario.log/attach` dentro de un step | Debajo de ese step |
| URL, código fuente, consola del navegador, video | En el nodo **Evidence** al final del escenario |
| Excepciones en hooks | En el nodo **Setup** o **Evidence** |

### Videos

Los videos se graban capturando periódicamente pantallas con WebDriver y codificándolas a H.264/MP4 con JCodec
(Java puro). Funciona con navegadores headless, Selenium Grid, proveedores en la nube y Appium, sin ffmpeg ni sesión
de escritorio.

- `autto.evidence.video=on-failure` (por defecto): se graban todos los escenarios y solo se conservan los fallidos.
- `autto.evidence.video-fps` equilibra fluidez y sobrecarga (por defecto 3).
- Solo se conserva la parte final indicada por `autto.evidence.video-max-duration`.
- El MP4 se reproduce dentro del reporte en Chrome, Edge, Firefox y Safari. Las builds open source de Chromium no
  incluyen el códec H.264; ahí usa el enlace *Download video*.
- Mientras se graba, `autto.browser.unhandled-prompt` es `ignore` por defecto para que las capturas en segundo plano nunca
  cierren alertas.

En Selenium Grid también puedes activar la grabación propia del Grid con la capability `se:recordVideo: true`.

## API Report

`io.github.andercmd.autto.core.report.Report` escribe en el step actual (thread-safe, no hace nada fuera de un
escenario, los secretos registrados se enmascaran):

```java
Report.info("Order created: " + id);
Report.pass("Payment accepted");             // etiqueta verde
Report.warning("Slow response: 4.2 s");      // marca el escenario como Warning en Extent
Report.fail("Soft failure, keep going");     // marca el step como fallido sin lanzar excepción
Report.screenshot("Cart before checkout");
Report.table("Order", Map.of("id", id, "total", total));
Report.table("Rows", List.of(List.of("a", "b"), List.of("c", "d")));
Report.json("Response", jsonBody);
Report.code("SQL", query);
Report.file("Invoice", pdfBytes, "pdf");
Report.video("Custom recording", mp4Bytes);
Report.author("jane.doe");
Report.html("<b>HTML de confianza</b>");
```

La API de Cucumber también funciona y llega a todos los reportes (Extent, HTML y JSON de Cucumber):

```java
scenario.log("texto");
scenario.attach(pngBytes, "image/png", "Screenshot");
scenario.attach(json.getBytes(UTF_8), "application/json", "payload");
scenario.attach(pdf, "application/pdf", "invoice.pdf");   // se guarda en attachments/ y se enlaza
```

## Personalización

- **Tema, título, nombre:** `autto.report.theme`, `autto.report.title`, `autto.report.name`.
- **Filas extra en el dashboard:** `autto.report.info.Release: 2.4.0`, `autto.report.info.Team: Payments`.
- **Estilos y scripts:** edita `autto-core/src/main/resources/autto/report/autto.css` y `autto.js`.
- **Historial:** `autto.report.timestamped=true` crea una carpeta por ejecución.
- **Archivo único portable:** `autto.report.screenshots-base64=true` incrusta las capturas en el HTML (los videos
  siguen siendo archivos).

La tabla de entorno del dashboard muestra perfiles, navegador, resolución del driver, destino, SO, Java y versiones de
Selenium, Cucumber y Spring Boot, además de la ejecución de CI. Los secretos nunca aparecen: las URLs se ocultan y
los secretos se enmascaran.

## Logs

`logs/autto.log` contiene cada línea de log con el hilo y el nombre del escenario (`%X{scenario}`), lo que hace
legibles las ejecuciones en paralelo, y los secretos se enmascaran (`%maskedMsg`). Usa `-Dautto.log.level=DEBUG`
para trazar cada interacción de `BasePage`.
