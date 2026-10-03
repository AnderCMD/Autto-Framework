<div align="center">

# Autto Framework

**Framework empresarial y open source de automatización QA**
**Spring Boot · Cucumber · Selenium · WebDriverManager · Extent Reports · Java 27**

[![CI](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml/badge.svg)](https://github.com/AnderCMD/Autto-Framework/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-27-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6db33f)
![Cucumber](https://img.shields.io/badge/Cucumber-8.0.4-23d96c)
![Selenium](https://img.shields.io/badge/Selenium-4.50.0-43b02a)

[English](README.md) · Español

</div>

---

Autto es un punto de partida listo para producción para la automatización de pruebas de UI. El motor (`autto-core`)
es una auto-configuración reutilizable de Spring Boot; la suite (`autto-e2e`) solo contiene funcionalidades de
negocio. Navegadores, drivers, secretos, paralelismo, evidencias y reportes ya están resueltos.

## Características principales

| | |
|---|---|
| **Spring Boot** | Inyección de dependencias con una instancia por escenario (`@PageObject`), perfiles por entorno, configuración tipada y validada con autocompletado en el IDE. |
| **Screaming architecture** | Pruebas organizadas por funcionalidad de negocio (`features/login`, `features/checkout`…), motor y suite en módulos separados. |
| **Cualquier navegador, sin importar qué** | Chrome, Chromium, Firefox, Edge, Safari. WebDriverManager → respaldo Selenium Manager → respaldo Docker si el navegador no está instalado. |
| **Cualquier plataforma** | Windows, macOS, Linux, navegadores en Docker, Selenium Grid, BrowserStack / Sauce Labs / LambdaTest, Android e iOS (Appium). |
| **Secretos seguros** | `.env` (ignorado por git) en local, secretos del CI en pipelines, valores enmascarados en logs y reportes, gitleaks en CI y pre-commit. |
| **Reportes completos** | Dashboard Extent Spark, línea de tiempo, tags, dispositivos, autores, reporte solo de fallos; salidas HTML/JSON/JUnit de Cucumber. |
| **Evidencias** | Capturas, **video MP4 de cada escenario** (sin ffmpeg), código fuente de la página, consola del navegador (BiDi), adjuntos. |
| **Quality gates** | Maven Enforcer, Checkstyle (sin `Thread.sleep`, sin `System.out`…), tests unitarios, perfil JaCoCo, Dependabot. |
| **Listo para CI** | GitHub Actions: escaneo de secretos, build, matriz SO × navegador, regresiones nocturnas en Grid y Docker. |

## Stack tecnológico

| Componente | Versión |
|---|---|
| Java | 27 (configurable con `java.version`, mínimo 21) |
| Spring Boot | 4.1.1 |
| Cucumber JVM (+ cucumber-spring) | 8.0.4 |
| Selenium | 4.50.0 |
| WebDriverManager | 6.4.0 |
| Appium Java client | 10.1.1 |
| JUnit Platform | 6.1.3 |
| Extent Reports | 5.1.2 |
| Checkstyle | 14.3.0 |
| Maven (wrapper) | 3.10.0 |

> **Sobre Java 27.** Su versión estable (GA) está prevista para marzo de 2027. Mientras tanto instala una
> [build Early Access](https://jdk.java.net/27/) o compila con cualquier JDK ≥ 21 usando `-Djava.version=25` (o 21).

## Inicio rápido

```bash
git clone https://github.com/AnderCMD/Autto-Framework.git
cd Autto-Framework

cp .env.example .env              # Windows: copy .env.example .env
# edita .env → SAUCE_PASSWORD=secret_sauce   (contraseña pública de la tienda demo)

./mvnw install                                                            # todo, con Chrome
./mvnw -pl autto-e2e test -Dautto.browser.name=firefox -Dcucumber.filter.tags=@smoke
./mvnw -pl autto-e2e test -Dspring.profiles.active=qa,docker             # navegadores en Docker
./mvnw install -Djava.version=21                                          # JDK anterior
```

Abre **`autto-e2e/target/autto-reports/index.html`**.

## Estructura del proyecto

```
Autto-Framework
├── pom.xml                         padre: versiones, plugins, quality gates
├── .env.example                    NOMBRES de los secretos (cópialo a .env, que está ignorado por git)
├── config/checkstyle/              estándar de código
├── docker-compose.yml              Selenium Grid
├── autto-core/                     MOTOR (librería publicable, auto-configuración de Spring Boot)
│   └── src/main/java/io/github/andercmd/autto/core/
│       ├── config/     AuttoProperties (tipado), AuttoSettings, DotEnv, perfiles
│       ├── spring/     AuttoAutoConfiguration
│       ├── driver/     DriverResolver (WebDriverManager → Selenium Manager → Docker), DriverFactory, DriverManager
│       ├── ui/         BasePage, @PageObject
│       ├── cucumber/   BrowserHooks (ciclo de vida del navegador + evidencias)
│       ├── media/      capturas, grabador de video
│       ├── report/     plugin Extent para Cucumber, API Report
│       ├── security/   enmascarado de secretos, Credentials
│       ├── data/       datos de prueba JSON, Datafaker
│       └── context/    ScenarioContext
└── autto-e2e/                      SUITE DE PRUEBAS de la aplicación bajo prueba
    └── src/test/
        ├── java/io/github/andercmd/autto/e2e/
        │   ├── CucumberTestSuite · CucumberSpringConfiguration · E2eTestApplication
        │   ├── shared/TestUsers.java
        │   └── features/           UNA CARPETA POR FUNCIONALIDAD DE NEGOCIO
        │       ├── login/      LoginPage, LoginSteps
        │       ├── inventory/  InventoryPage, InventorySteps
        │       ├── checkout/   CartPage, CheckoutPage, CheckoutSteps, Customer
        │       └── showcase/   ReportShowcaseSteps (demo de la API de reportes)
        └── resources/
            ├── application.yml + application-{qa,staging,prod,ci,docker,grid,browserstack}.yml
            ├── features/<feature>/*.feature
            ├── junit-platform.properties
            └── logback-test.xml
```

## Documentación

| Guía | Descripción |
|---|---|
| [Primeros pasos](docs/es/primeros-pasos.md) | Requisitos, primera ejecución, IDE |
| [Arquitectura](docs/es/arquitectura.md) | Módulos, screaming architecture, inyección de dependencias, flujo |
| [Decisiones de arquitectura](docs/es/decisiones.md) | Por qué Spring Boot, WebDriverManager, `.env` y multi-módulo |
| [Escribir pruebas](docs/es/escribir-pruebas.md) | Nueva funcionalidad paso a paso, page objects, usuarios, datos, beans propios |
| [Configuración](docs/es/configuracion.md) | Perfiles, precedencia y todas las claves `autto.*` |
| [Secretos y variables de entorno](docs/es/secretos.md) | `.env`, secretos del CI, enmascarado, prevención de fugas |
| [Ejecución](docs/es/ejecucion.md) | Tags, navegadores, paralelismo, Docker, Grid, nube, Appium |
| [Reportes y evidencias](docs/es/reportes.md) | Reporte Extent, capturas, videos, API Report |
| [CI/CD](docs/es/ci-cd.md) | GitHub Actions, Jenkins, GitLab, Azure DevOps |
| [Solución de problemas](docs/es/solucion-de-problemas.md) | Problemas comunes y soluciones |
| [Contribuir](CONTRIBUTING.md) | Cómo contribuir |

## Aplicación de ejemplo

Los escenarios de ejemplo usan [saucedemo.com](https://www.saucedemo.com). Sustituye `features/*`,
`test-data.users` y `autto.base-url` por los de tu aplicación. El escenario `@demo-failure` falla a propósito para
mostrar las evidencias de un fallo:

```bash
./mvnw -pl autto-e2e test -Dcucumber.filter.tags=@demo-failure
```

## Licencia

[MIT](LICENSE) © AnderCMD y colaboradores.
