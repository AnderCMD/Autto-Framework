# Primeros pasos

[← Volver al README](../../README.es.md) · [English](../en/getting-started.md)

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 27 (o ≥ 21 con `-Djava.version=<n>`) | Java 27 GA está prevista para marzo de 2027. Mientras tanto usa una [build Early Access](https://jdk.java.net/27/). |
| Maven | no requerido | El Maven Wrapper (`./mvnw`, `mvnw.cmd`) descarga Maven 3.10.0. |
| Navegador | Chrome, Chromium, Firefox, Edge o Safari | Los drivers se resuelven solos (WebDriverManager → Selenium Manager). |
| Docker | opcional | Navegadores en contenedores (perfil `docker`) o Selenium Grid local. |
| Appium 2+ | opcional | Móvil (`autto.execution.target=appium`). |

## Primera ejecución

```bash
git clone https://github.com/AnderCMD/Autto-Framework.git
cd Autto-Framework

# 1. Secretos: crea tu .env local (ignorado por git)
cp .env.example .env                       # Windows: copy .env.example .env
#    y define SAUCE_PASSWORD=secret_sauce (contraseña pública de la tienda demo)

# 2. Build + tests unitarios del framework + chequeo del reporte sin navegador
./mvnw verify -Dcucumber.filter.tags=@showcase

# 3. Suite completa con Chrome
./mvnw install
```

Abre **`autto-e2e/target/autto-reports/index.html`**.

> `./mvnw install` construye `autto-core` y luego ejecuta la suite. Para ejecutar solo la suite cuando el core ya
> está instalado: `./mvnw -pl autto-e2e test`.

## Revisa tu entorno

```bash
./scripts/doctor.sh
```

Indica qué falta (JDK, `.env`, navegadores, Docker, que `autto.base-url` responda) antes de perder tiempo en una
ejecución que falla.

## Compilar con un JDK anterior a 27

```bash
./mvnw install -Djava.version=25
```

Hazlo permanente en tu máquina con `.mvn/maven.config` (no lo subas si tu equipo usa JDK 27):

```
-Djava.version=25
```

## Configurar el IDE

### IntelliJ IDEA

1. *File → Open* → el `pom.xml` raíz (abrir como proyecto).
2. Plugins: **Cucumber for Java**, **Gherkin**, **Spring** (Ultimate) o **Spring Boot Assistant** (Community) para
   autocompletar `application.yml`.
3. El estilo de código se toma de `.editorconfig` automáticamente.
4. Ejecuta `CucumberTestSuite`, o clic derecho en un `.feature` → *Run* (define el glue
   `io.github.andercmd.autto.e2e io.github.andercmd.autto.core.cucumber` en la plantilla de Cucumber).
5. IntelliJ lee el `.env` a través del propio framework, sin plugins.

### VS Code

Extensiones: *Extension Pack for Java*, *Spring Boot Extension Pack*, *Cucumber (Gherkin) Full Support*.

```json
{
  "cucumberautocomplete.steps": ["autto-e2e/src/test/java/**/*.java"],
  "cucumberautocomplete.syncfeatures": "autto-e2e/src/test/resources/features/**/*.feature"
}
```

## Hazlo tuyo

1. Renombra los paquetes `io.github.andercmd.autto` y el `groupId` de los POM. Actualiza `cucumber.glue`.
2. Renombra `autto-e2e` con el nombre de tu aplicación (`tienda-e2e`) y define `autto.base-url` en
   `application-<entorno>.yml`.
3. Sustituye las carpetas demo `features/*` y `test-data.users` por las tuyas.
4. Pon los nombres de tus secretos en `.env.example` (sin valores) y los valores en el almacén de secretos del CI.
5. Opcional: `pre-commit install` para escanear secretos antes de cada commit.
