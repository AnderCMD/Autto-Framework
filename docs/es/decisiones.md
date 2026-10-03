# Decisiones de arquitectura

[← Volver al README](../../README.es.md) · [English](../en/decisions.md)

Registros de decisiones de arquitectura (ADR) ligeros: qué se decidió, por qué y qué cuesta.

## ADR-001 · Spring Boot como plataforma de pruebas — **aceptada**

**Contexto.** Una plantilla usada por varios equipos necesita inyección de dependencias, configuración por entorno,
ajustes tipados y validados, y una forma sencilla de conectar clientes de API, utilidades de base de datos o
generadores de datos de prueba.

**Decisión.** Usar Spring Boot 4 (`cucumber-spring`) como contenedor de pruebas. `autto-core` es una
*auto-configuración* de Spring Boot: basta con depender de él para tener el motor.

**Beneficios**

- Inyección por constructor con **una instancia por escenario** (`@ScenarioScope`, `@PageObject`): seguro en paralelo.
- `application.yml` + **perfiles** (`qa`, `staging`, `ci`, `grid`, `docker`, `browserstack`…) combinables en ejecución.
- **Configuración tipada y validada** (`AuttoProperties`) con autocompletado en el IDE y errores inmediatos.
- Relaxed binding: cualquier clave se sobrescribe con variables `AUTTO_*` o propiedades `-D`.
- Todo el ecosistema Spring a una dependencia de distancia (clientes REST, JDBC, mensajería, Vault…) para preparar
  datos y validar el back-end.

**Costes.** ~1–2 s de arranque del contexto una vez por ejecución y más dependencias. Aceptable en suites de UI cuyos
escenarios tardan segundos cada uno.

**Cuándo NO usarlo.** Una suite mínima de una persona, sin entornos ni integraciones; PicoContainer bastaría.

## ADR-002 · WebDriverManager primero, Selenium Manager como respaldo — **aceptada**

**Contexto.** Selenium Manager (incluido en Selenium) ya resuelve drivers, pero las redes empresariales añaden
proxies, mirrors, cachés y máquinas sin el navegador solicitado.

**Decisión.** `autto.driver.resolution=webdrivermanager` (por defecto) con `autto.driver.fallback=true`:

1. WebDriverManager detecta el navegador instalado y descarga/cachea el driver compatible (proxy, mirror y caché;
   se resuelve una vez por ejecución).
2. Si falla, Selenium Manager toma el relevo automáticamente (incluso puede descargar Chrome/Firefox).
3. Con `autto.driver.docker-fallback=true`, un navegador no instalado arranca en Docker.
4. `execution.target=docker` ejecuta siempre los navegadores en contenedores desechables: solo se necesita Docker.

Si se pide una versión/canal concreto (`autto.browser.version=beta`) se usa Selenium Manager directamente porque
puede descargar ese navegador.

## ADR-003 · Secretos: `.env` en local, almacén de secretos en CI — **aceptada**

**Contexto.** Las credenciales (usuarios de prueba, claves de nube) deben poder usarse en las pruebas y nunca llegar
al repositorio.

**Opciones evaluadas**

| Opción | Veredicto |
|---|---|
| Valores en `application.yml` / JSON | ❌ Terminan en el historial de git. |
| Solo variables de entorno del sistema | ✅ Seguro, pero incómodo en local (muchas variables, por terminal, por IDE). |
| **`.env` (ignorado por git) + `.env.example` (versionado)** | ✅ Simple para el desarrollador, estándar en la industria, mismos nombres que los secretos del CI. |
| Gestor de secretos (Vault, AWS/Azure/GCP secret managers, 1Password CLI) | ✅✅ Lo mejor para empresas: rotación, auditoría, sin archivos. Recomendado como siguiente paso; Spring Cloud Vault / Azure Key Vault se conectan con los mismos nombres de propiedades. |

**Decisión.** `.env` para desarrollo local, secretos del CI como variables de entorno con **los mismos nombres**, y
un gestor de secretos cuando la organización lo tenga. Defensa en profundidad:

- `.env` y `.env.*` están en `.gitignore` (salvo `.env.example`, que no contiene valores).
- `application.yml` solo contiene referencias: `password: ${SAUCE_PASSWORD}`.
- Las variables de entorno reales siempre sobrescriben `.env`.
- Los valores secretos se **enmascaran** (`******`) en logs y en el reporte Extent.
- **gitleaks** se ejecuta en el CI (todo el historial) y como hook pre-commit.

## ADR-004 · Build multi-módulo — **aceptada**

`autto-core` (motor versionado, publicable en un repositorio Maven) y `autto-e2e` (la suite de una aplicación).
Otros equipos crean su propio proyecto `*-e2e` que depende de `autto-core`, y las mejoras del motor llegan a todos
subiendo una versión.
Ver la [Guía de adopción](adopcion.md) para consumir `autto-core` mediante JitPack o un repositorio interno.

## ADR-005 · REST Assured para pruebas de API — **aceptada**

**Contexto.** Las suites reales mezclan UI y API: las APIs preparan datos en segundos, verifican el estado del
back-end y prueban servicios directamente. Los equipos de QA lo necesitan integrado, en el reporte y enmascarado igual
que la parte de UI.

**Opciones consideradas**

| Opción | Veredicto |
|---|---|
| `RestClient` de Spring | ✅ Ligero y ya en el ecosistema, pero no es un DSL de pruebas (sin `then().statusCode()` ni aserciones JSON path). |
| `java.net.http.HttpClient` | ❌ Demasiado bajo nivel para código de pruebas. |
| **REST Assured** | ✅ Estándar de facto en automatización de APIs, conocido por la mayoría de QA, DSL fluido given/when/then. |

**Decisión.** REST Assured detrás del bean `Api`: URL base, cabeceras por defecto, timeouts y un filtro que escribe
cada intercambio en el reporte con las cabeceras sensibles y los secretos enmascarados. **Coste:** Groovy y Apache
HttpClient 4 en el classpath de pruebas (~10 MB), aceptable para un framework de pruebas.

## ADR-006 · axe-core para accesibilidad — **aceptada**

**Contexto.** La accesibilidad (WCAG 2.1 AA, European Accessibility Act, ADA) es un requisito legal para muchas
empresas y es más barato detectarla en la misma suite que ya abre cada página.

**Decisión.** La integración de Selenium de `axe-core` de Deque (`Accessibility.scan()`), el motor open source más
usado, con los tags WCAG y el impacto que falla configurables en `autto.accessibility.*`. Las auditorías solo se
ejecutan donde un step las pide, así que nunca ralentizan otros escenarios.

## ADR-007 · Resiliencia por defecto — **aceptada**

- **Reintentos al arrancar el navegador** (`autto.driver.start-retries`, back-off exponencial): un Grid ocupado o la
  cola de la nube fallan la creación de la sesión, no la prueba. Los errores de configuración nunca se reintentan.
- **Aserciones suaves** verificadas automáticamente al final de cada escenario: todos los fallos en una ejecución.
- **Sin reintentos de escenarios fallidos** dentro de la ejecución: ocultan bugs reales. Re-ejecuta los fallos de
  forma explícita con `rerun.txt` e investiga la inestabilidad.
