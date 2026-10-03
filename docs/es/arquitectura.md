# Arquitectura

[← Volver al README](../../README.es.md) · [English](../en/architecture.md)

## Screaming architecture

> "Tu arquitectura debe contarle al lector sobre el sistema, no sobre los frameworks que usaste." — Robert C. Martin

Al abrir `src/test/java/.../features` ves **qué hace el producto** (`login`, `inventory`, `checkout`), no capas
técnicas como `pages/`, `steps/`, `models/`. Todo lo necesario para una funcionalidad de negocio vive junto:

```
features/checkout/
├── CartPage.java          page object(s) de la funcionalidad
├── CheckoutPage.java
├── CheckoutSteps.java     step definitions de la funcionalidad
└── Customer.java          modelos / builders de la funcionalidad

resources/features/checkout/checkout.feature    Gherkin, mismo nombre de carpeta
resources/testdata/checkout/...                 datos, mismo nombre de carpeta
```

Beneficios:

- **Cohesión**: cambiar una funcionalidad toca una sola carpeta.
- **Ownership**: cada squad puede ser dueño de sus carpetas (`CODEOWNERS`).
- **Escalabilidad**: cientos de funcionalidades no terminan en un único paquete `pages/` con cientos de clases.
- **Borrar es fácil**: eliminar una funcionalidad es eliminar una carpeta.

La reutilización entre funcionalidades es explícita: `CheckoutSteps` inyecta `InventoryPage` de
`features.inventory`. El código genérico vive en `shared/` (lado de pruebas) o en el `core` del framework (lado main).

## Capas

```
┌──────────────────────────────────────────────────────────────────────┐
│ src/test  ── QUÉ se prueba                                           │
│   features/<feature>/*.feature   lenguaje de negocio (Gherkin)       │
│   features/<feature>/*Steps      glue: Gherkin → page objects        │
│   features/<feature>/*Page       UI: locators y acciones             │
│   shared/hooks                   navegador + evidencias              │
├──────────────────────────────────────────────────────────────────────┤
│ src/main  ── CÓMO (core reutilizable, sin negocio)                   │
│   config  driver  ui  media  report  data  context                   │
├──────────────────────────────────────────────────────────────────────┤
│ Librerías: Cucumber · JUnit Platform · Selenium · Appium · Extent    │
└──────────────────────────────────────────────────────────────────────┘
```

Regla de dependencias: `features` → `core` → librerías. El core nunca depende del código de pruebas.

### Paquetes del core

| Paquete | Responsabilidad | Clases clave |
|---|---|---|
| `core.config` | Configuración por capas con getters tipados | `AuttoConfig`, `ConfigKeys` |
| `core.driver` | Crea y gestiona navegadores/dispositivos por hilo | `DriverFactory`, `BrowserOptionsFactory`, `DriverManager`, `DriverSession`, `CapabilitiesParser` |
| `core.ui` | Page object base con esperas explícitas y acciones seguras | `BasePage` |
| `core.media` | Captura de evidencias | `Screenshots`, `VideoRecorder`, `EvidenceMode` |
| `core.report` | Generación del reporte Extent y API pública de logging | `ExtentCucumberPlugin`, `ExtentReportManager`, `Report`, `ReportPaths` |
| `core.data` | Datos JSON con `${placeholders}` y datos aleatorios | `TestData` |
| `core.context` | Estado por escenario compartido entre clases de steps | `ScenarioContext` |

## Flujo de ejecución

```
mvn test
 └─ Surefire → JUnit Platform → CucumberTestSuite (@Suite, engine "cucumber")
     ├─ ExtentCucumberPlugin  ← recibe cada evento de Cucumber (thread-safe)
     └─ por cada escenario (opcionalmente en hilos paralelos)
         ├─ @Before  BrowserHooks.startBrowser
         │     DriverManager.start() → DriverFactory → local | remote | appium
         │     VideoRecorder.start()  (si video.mode != off)
         ├─ steps → page objects → DriverManager.driver()
         │     @AfterStep captura (screenshot.mode)
         └─ @After BrowserHooks.collectEvidenceAndQuit
               URL, page source, consola, video → scenario.attach(...)
               DriverManager.quit()
 └─ TestRunFinished → flush de Extent → target/autto-reports/index.html
```

## Decisiones de diseño

| Decisión | Motivo |
|---|---|
| Constantes `By` en lugar de `@FindBy` / `PageFactory` | Page objects sin estado y seguros en paralelo; sin proxies obsoletos. |
| Las páginas obtienen el driver de forma perezosa (`DriverManager.driver()`) | PicoContainer puede crear páginas antes de que exista el navegador; un navegador por hilo. |
| Espera implícita = 0, esperas explícitas siempre | Mezclar ambas causa timeouts impredecibles. |
| Plugin propio de Extent en lugar del adaptador de terceros | Control total del reporte, compatible con Cucumber 8, paralelo, videos y evidencias de hooks. |
| Video desde capturas WebDriver + JCodec | Funciona en todas partes (headless, Grid, nube, Appium) sin ffmpeg ni escritorio. |
| Properties + variables de entorno + `-D` | Cambiar entre local, CI y nube sin tocar código; los secretos nunca se suben. |
| PicoContainer | Inyección por constructor ligera; un grafo de objetos nuevo por escenario. |
