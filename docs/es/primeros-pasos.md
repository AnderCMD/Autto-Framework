# Primeros pasos

[← Volver al README](../../README.es.md) · [English](../en/getting-started.md)

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 27 (o ≥ 21 con `-Djava.version=<n>`) | Java 27 GA está prevista para marzo de 2027. Mientras tanto usa una [build Early Access](https://jdk.java.net/27/). |
| Maven | no requerido | El Maven Wrapper incluido (`./mvnw`, `mvnw.cmd`) descarga Maven 3.9.16. |
| Navegador | Chrome, Firefox, Edge o Safari | Selenium Manager resuelve los drivers automáticamente y puede descargar Chrome/Firefox si faltan. |
| Docker | opcional | Solo para el Selenium Grid local (`docker compose up`). |
| Appium 2+ | opcional | Solo para móvil (`execution.target=appium`). |

Comprueba tu entorno:

```bash
java -version
./mvnw -v          # Windows: mvnw.cmd -v
```

## Primera ejecución

```bash
# 1. Tests unitarios del framework (sin navegador, unos segundos)
./mvnw -Punit test

# 2. Pipeline de reportes sin navegador
./mvnw test -Dcucumber.filter.tags=@showcase

# 3. Suite completa con Chrome
./mvnw test
```

Los reportes se generan en `target/autto-reports/`:

| Archivo | Contenido |
|---|---|
| `index.html` | Reporte Extent Spark (abre este) |
| `failed.html` | El mismo reporte, solo fallos y advertencias |
| `cucumber/cucumber.html` | Reporte HTML nativo de Cucumber |
| `cucumber/cucumber.json`, `cucumber-junit.xml`, `cucumber.ndjson` | Salidas para herramientas de CI |
| `rerun.txt` | Escenarios fallidos, para re-ejecutarlos |
| `logs/autto.log` | Log completo de la ejecución |

## Compilar con un JDK anterior a 27

La propiedad `java.version` controla `--release` y la regla del Maven Enforcer:

```bash
./mvnw test -Djava.version=25
```

Para hacerlo permanente en tu máquina, añádelo a `.mvn/maven.config` (crea el archivo):

```
-Djava.version=25
```

## Configurar el IDE

### IntelliJ IDEA

1. *File → Open* y selecciona `pom.xml` (abrir como proyecto).
2. Instala los plugins **Cucumber for Java** y **Gherkin**.
3. Ejecuta `CucumberTestSuite` o haz clic derecho en un `.feature` → *Run*.
   Para ejecutar archivos `.feature`, define el glue `io.github.andercmd.autto` en la plantilla de configuración.

### VS Code

1. Instala *Extension Pack for Java* y *Cucumber (Gherkin) Full Support*.
2. Añade a `.vscode/settings.json`:

```json
{
  "cucumberautocomplete.steps": ["src/test/java/**/*.java"],
  "cucumberautocomplete.syncfeatures": "src/test/resources/features/**/*.feature"
}
```

### Eclipse

Importa como *Existing Maven Project* e instala *Cucumber Eclipse Plugin* desde el marketplace.

## Hazlo tuyo

1. Renombra el paquete `io.github.andercmd.autto` (refactor del IDE) y actualiza `groupId` / `artifactId` en
   `pom.xml` y `cucumber.glue` en `junit-platform.properties`.
2. Define `base.url` en `src/test/resources/environments/*.properties`.
3. Borra las carpetas de demo dentro de `features/` (conserva `showcase` si quieres un chequeo sin navegador).
4. Crea tu primera funcionalidad: ver [Escribir pruebas](escribir-pruebas.md).
