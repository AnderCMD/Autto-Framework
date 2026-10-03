<div align="center">

# Autto Framework

**Plantilla open source de automatización QA · Cucumber · Selenium · Extent Reports · Java 27**

[![CI](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml/badge.svg)](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-27-orange)
![Cucumber](https://img.shields.io/badge/Cucumber-8.0.4-23d96c)
![Selenium](https://img.shields.io/badge/Selenium-4.50.0-43b02a)

[English](README.md) · Español

</div>

---

Autto es un punto de partida listo para producción para la automatización de pruebas de UI. Clónalo, renombra el
paquete y empieza a escribir escenarios: navegadores, drivers, ejecución en paralelo, evidencias y reportes ya están
resueltos.

## Características principales

| | |
|---|---|
| **BDD** | Cucumber 8 sobre el motor JUnit Platform 6, inyección de dependencias con PicoContainer, escenarios en paralelo. |
| **Screaming architecture** | El código se organiza por funcionalidad de negocio (`features/login`, `features/checkout`…), no por capa técnica. |
| **Cualquier navegador** | Chrome, Firefox, Edge y Safari. Los drivers se resuelven automáticamente con Selenium Manager (incluso descarga Chrome/Firefox si faltan). |
| **Cualquier plataforma** | Windows, macOS, Linux, Docker, Selenium Grid, BrowserStack / Sauce Labs / LambdaTest, Android e iOS (Appium). |
| **Reportes completos** | Dashboard Extent Spark con línea de tiempo, tags, dispositivos, autores, reporte solo de fallos y archivo JSON. |
| **Evidencias** | Capturas de pantalla, **video MP4 de cada escenario** (sin ffmpeg), código fuente de la página, consola del navegador (BiDi), adjuntos. |
| **Configuración** | Un archivo de propiedades + archivos por entorno, sobrescribibles con variables `AUTTO_*` o flags `-D`. |
| **Listo para CI** | Matriz de GitHub Actions (Windows/macOS/Linux × Chrome/Firefox/Edge + Safari), regresión nocturna en Grid, Dependabot, Maven Wrapper. |

## Stack tecnológico

| Componente | Versión |
|---|---|
| Java | 27 (configurable con `java.version`) |
| Cucumber JVM | 8.0.4 |
| Selenium | 4.50.0 |
| Appium Java client | 10.1.1 |
| JUnit Platform | 6.1.3 |
| Extent Reports | 5.1.2 |
| AssertJ · Datafaker · Jackson · Awaitility · Logback | última versión estable |
| Maven (wrapper) | 3.9.16 |

> **Sobre Java 27.** El proyecto apunta a Java 27, cuya versión estable (GA) está prevista para marzo de 2027. Mientras
> tanto, instala una build Early Access desde [jdk.java.net/27](https://jdk.java.net/27/) o compila con cualquier
> JDK ≥ 21 pasando `-Djava.version=25` (o 21). El código solo usa características del lenguaje disponibles desde Java 21.

## Inicio rápido

```bash
git clone https://github.com/AnderCMD/Autto-Framework.git
cd Autto-Framework

# Todo (tests unitarios + escenarios) con Chrome
./mvnw test

# Escenarios smoke en Firefox headless
./mvnw test -Dbrowser=firefox -Dbrowser.headless=true -Dcucumber.filter.tags=@smoke

# Con un JDK anterior
./mvnw test -Djava.version=21
```

Al terminar, abre **`target/autto-reports/index.html`**.

## Estructura del proyecto

```
src
├── main/java/io/github/andercmd/autto/core      ← framework reutilizable (no pongas tests aquí)
│   ├── config      configuración por capas (AuttoConfig, ConfigKeys)
│   ├── driver      fábrica de navegadores/dispositivos, opciones, DriverManager thread-safe
│   ├── media       capturas de pantalla y grabador de video
│   ├── report      plugin Extent para Cucumber, API Report, carpetas del reporte
│   ├── ui          BasePage con esperas explícitas
│   ├── data        datos de prueba JSON + Datafaker
│   └── context     ScenarioContext compartido entre steps
└── test
    ├── java/io/github/andercmd/autto
    │   ├── CucumberTestSuite.java              ← punto de entrada
    │   ├── shared/hooks/BrowserHooks.java      ← ciclo de vida del navegador + evidencias
    │   └── features                            ← UNA CARPETA POR FUNCIONALIDAD DE NEGOCIO
    │       ├── login/      LoginPage, LoginSteps, Credentials
    │       ├── inventory/  InventoryPage, InventorySteps
    │       ├── checkout/   CartPage, CheckoutPage, CheckoutSteps, Customer
    │       └── showcase/   demo del reporte sin navegador
    └── resources
        ├── autto.properties                    ← configuración principal
        ├── environments/{qa,staging,prod}.properties
        ├── features/<feature>/*.feature        ← Gherkin, mismas carpetas que el código
        ├── testdata/<feature>/*.json
        ├── junit-platform.properties           ← opciones de Cucumber
        └── logback-test.xml
```

## Documentación

| Guía | Descripción |
|---|---|
| [Primeros pasos](docs/es/primeros-pasos.md) | Requisitos, instalación, primera ejecución, IDE |
| [Arquitectura](docs/es/arquitectura.md) | Screaming architecture, capas, flujo de ejecución |
| [Escribir pruebas](docs/es/escribir-pruebas.md) | Añadir una funcionalidad paso a paso, page objects, datos, contexto |
| [Configuración](docs/es/configuracion.md) | Todas las claves de configuración |
| [Ejecución](docs/es/ejecucion.md) | Navegadores, tags, paralelismo, Grid, Docker, nube, Appium |
| [Reportes y evidencias](docs/es/reportes.md) | Reporte Extent, capturas, videos, API Report |
| [CI/CD](docs/es/ci-cd.md) | GitHub Actions, Jenkins, GitLab, Azure DevOps |
| [Solución de problemas](docs/es/solucion-de-problemas.md) | Problemas comunes y soluciones |
| [Contribuir](CONTRIBUTING.md) | Cómo contribuir |

## Aplicación de ejemplo

Los escenarios de ejemplo usan [saucedemo.com](https://www.saucedemo.com), una tienda de demostración pública.
Sustituye las carpetas `features/*` por las funcionalidades de tu aplicación y cambia `base.url` en
`environments/*.properties`. El escenario `@demo-failure` falla a propósito; ejecútalo para ver las evidencias de un
fallo en el reporte:

```bash
./mvnw test -Dcucumber.filter.tags=@demo-failure
```

## Licencia

[MIT](LICENSE) © AnderCMD y colaboradores.
