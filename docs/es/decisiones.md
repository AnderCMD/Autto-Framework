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
