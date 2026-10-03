# Arquitectura

[← Volver al README](../../README.es.md) · [English](../en/architecture.md)

## Módulos

```
autto-parent (pom)                    versiones, plugins, quality gates (enforcer, checkstyle, jacoco)
├── autto-core   (jar, publicable)    el motor, empaquetado como auto-configuración de Spring Boot
└── autto-e2e    (solo pruebas)       la suite de UNA aplicación bajo prueba (demo: saucedemo.com)
```

`autto-core` no conoce ningún negocio; `autto-e2e` no sabe nada de drivers, videos ni reportes. Una empresa publica
`autto-core` una vez y cada equipo de producto crea su propio proyecto `<producto>-e2e` que depende de él.

## Screaming architecture

> "Tu arquitectura debe contarle al lector sobre el sistema, no sobre los frameworks que usaste." — Robert C. Martin

Dentro de `autto-e2e` ves **qué hace el producto**, no capas técnicas:

```
autto-e2e/src/test/java/io/github/andercmd/autto/e2e/
├── CucumberTestSuite.java             punto de entrada (suite de JUnit Platform)
├── CucumberSpringConfiguration.java   puente Cucumber ↔ Spring
├── E2eTestApplication.java            configuración Spring de la suite (añade aquí tus beans)
├── shared/TestUsers.java              soporte transversal (usuarios de application.yml + secretos)
└── features/
    ├── login/       LoginPage, LoginSteps
    ├── inventory/   InventoryPage, InventorySteps
    └── checkout/    CartPage, CheckoutPage, CheckoutSteps, Customer

autto-e2e/src/test/resources/
├── application.yml, application-<perfil>.yml
├── features/<feature>/*.feature       mismos nombres de carpeta que el código
└── testdata/<feature>/*.json
```

Todo lo de una funcionalidad vive junto → alta cohesión, ownership claro (`CODEOWNERS` por carpeta), fácil de borrar.
La reutilización entre funcionalidades es explícita (inyección por constructor de la página de otra funcionalidad).

## Capas y regla de dependencias

```
features (Gherkin, steps, páginas)  ──►  autto-core  ──►  Spring Boot · Cucumber · Selenium · WebDriverManager · Appium · Extent
```

### Paquetes de `autto-core`

| Paquete | Responsabilidad | Clases clave |
|---|---|---|
| `config` | Configuración basada en Spring Boot, perfiles, `.env` | `AuttoProperties`, `AuttoSettings`, `DotEnv`, `DotEnvEnvironmentPostProcessor` |
| `spring` | Auto-configuración (beans para los proyectos de prueba) | `AuttoAutoConfiguration` |
| `driver` | Resolución y creación de drivers por hilo | `DriverResolver` (WDM → Selenium Manager), `DriverFactory`, `DriverManager`, `DriverSession`, `BrowserOptionsFactory`, `CapabilitiesParser` |
| `ui` | Page object base y estereotipo | `BasePage`, `@PageObject` |
| `cucumber` | Glue del framework | `BrowserHooks` |
| `media` | Captura de evidencias | `Screenshots`, `VideoRecorder` |
| `report` | Reporte Extent + API pública de logging | `ExtentCucumberPlugin`, `ExtentReportManager`, `Report`, `ReportPaths` |
| `security` | Enmascarado de secretos | `Secrets`, `Credentials`, `MaskingMessageConverter` |
| `data` | Datos JSON, datos aleatorios | `TestData` |
| `context` | Estado compartido por escenario | `ScenarioContext` |

## Inyección de dependencias

| Bean | Scope | Lo provee |
|---|---|---|
| `AuttoSettings`, `AuttoProperties` | singleton | `AuttoAutoConfiguration` |
| `ScenarioContext` | escenario | `AuttoAutoConfiguration` |
| Clases `@PageObject` | escenario | component scan de `E2eTestApplication` |
| Step definitions, hooks | escenario | `cucumber-spring` (glue) |
| `TestUsers`, tus clientes de API, builders… | singleton (o escenario) | component scan |

El contexto de Spring se crea una vez por ejecución y lo comparten todos los escenarios e hilos; los beans de scope
escenario se recrean en cada escenario, por eso la ejecución en paralelo es segura.

## Flujo de ejecución

```
./mvnw install
 └─ autto-e2e: Surefire → JUnit Platform → CucumberTestSuite (engine "cucumber")
     ├─ ExtentCucumberPlugin ← cada evento de Cucumber (thread-safe), carga AuttoSettings
     ├─ Contexto Spring (una vez) ← application.yml + perfil + .env + variables de entorno + -D
     └─ por cada escenario (opcionalmente en paralelo)
         ├─ @Before BrowserHooks.startBrowser
         │     DriverFactory: local (WDM → Selenium Manager → Docker) | docker | remote | appium
         │     VideoRecorder.start()
         ├─ steps (beans de escenario) → páginas @PageObject → DriverManager.driver()
         │     @AfterStep captura
         └─ @After evidencias (URL, código fuente, consola, video) → quit
 └─ TestRunFinished → flush de Extent → autto-e2e/target/autto-reports/index.html
```

## Decisiones de diseño

Ver [Decisiones de arquitectura (ADR)](decisiones.md) sobre Spring Boot, WebDriverManager, secretos y multi-módulo.
Otras decisiones:

| Decisión | Motivo |
|---|---|
| Constantes `By` en lugar de `@FindBy` | Page objects sin estado y seguros en paralelo. |
| Driver obtenido de forma perezosa (`DriverManager.driver()`) | Las páginas se crean antes que el navegador; un navegador por hilo. |
| Espera implícita 0, esperas explícitas | Timeouts predecibles (lo exige Checkstyle). |
| Plugin Extent propio | Control total, Cucumber 8, paralelo, videos, enmascarado. |
| Video desde capturas WebDriver + JCodec | Funciona headless, en Docker, Grid, nube y Appium sin ffmpeg. |
