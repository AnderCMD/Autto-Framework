# Guía de adopción

[← Volver al README](../../README.es.md) · [English](../en/adoption.md)

Autto escala desde una sola persona automatizando una aplicación web hasta decenas de equipos compartiendo un mismo
motor. Empieza en el nivel que necesitas hoy; subir de nivel nunca exige reescribir.

## Elige tu nivel

| | **Inicial** | **Equipo** | **Empresa** |
|---|---|---|---|
| Quién | 1–2 personas, una aplicación | Un equipo de producto | Muchos equipos y productos |
| Cómo empezar | *Use this template* / clonar | Plantilla + CI | Proyectos `<producto>-e2e` que dependen de `autto-core` |
| Entornos | Perfil `qa` | Perfiles `qa`, `staging`, `ci` | Un perfil por entorno y destino de ejecución |
| Ejecución | Chrome local | Matriz de CI, escenarios en paralelo | Selenium Grid / proveedor cloud, regresiones nocturnas |
| Tipos de prueba | UI | UI + preparación de datos por API | UI + API + accesibilidad + móvil |
| Secretos | `.env` | `.env` + secretos del CI | Gestor de secretos (Vault, Azure Key Vault, AWS Secrets Manager) |
| Actualizar el motor | — | Fusionar las versiones del repositorio original | Subir la versión de `autto-core` |

### Inicial — primer escenario en 15 minutos

1. Pulsa **Use this template** en GitHub (o clona el repositorio), **o** genera un proyecto independiente y ligero:
   `./scripts/new-project.sh ../mi-e2e com.acme mi-e2e` (ver [Pruebas avanzadas](pruebas-avanzadas.md#empezar-un-proyecto-nuevo)).
   Abre el repositorio en un dev container / Codespace para tener un entorno listo.
2. Sigue [Primeros pasos](primeros-pasos.md) y ejecuta la demo una vez.
3. Apunta `autto.base-url` a tu aplicación, borra los `features/*` de la demo y escribe tu primera funcionalidad
   ([Escribir pruebas](escribir-pruebas.md)).
4. Mantén los valores por defecto: Chrome local, videos y capturas en los fallos, reporte Extent.

### Equipo — CI, entornos y velocidad

- Un `application-<entorno>.yml` por entorno; los secretos como secretos del CI con los nombres de `.env.example`.
- Activa los workflows de GitHub Actions incluidos (o copia los ejemplos de Jenkins / GitLab / Azure DevOps de
  [CI/CD](ci-cd.md)).
- Etiqueta los escenarios (`@smoke`, `@regression`, tags por funcionalidad) y ejecuta `@smoke` en cada pull request.
- Activa la ejecución en paralelo (`cucumber.execution.parallel.enabled=true`) cuando los escenarios sean
  independientes.
- Crea los datos de prueba por API ([Pruebas de API](pruebas-api.md)) en vez de por la UI.
- Asigna responsables por carpeta de funcionalidad en `CODEOWNERS`.

### Empresa — un motor, muchas suites

Publica `autto-core` una vez y deja que cada equipo de producto sea dueño de un proyecto de suite pequeño. Las
correcciones y mejoras del motor llegan a todos subiendo una versión, y nadie hace un fork del motor.

```
empresa/autto-core           (este repositorio o tu fork)  →  publicado en JitPack / Nexus / Artifactory
empresa/checkout-e2e         depende de autto-core
empresa/backoffice-e2e       depende de autto-core
empresa/mobile-app-e2e       depende de autto-core (execution.target=appium)
```

Añadidos recomendados: Selenium Grid o un proveedor cloud (perfiles `grid` / `browserstack`), un gestor de secretos,
regresiones `@regression` nocturnas, auditorías de accesibilidad en las páginas clave y una librería compartida de
beans de API para los datos de prueba.

## Usar `autto-core` desde tu propio proyecto

### 1. Obtén el artefacto

**JitPack (sin configuración, público).** Cada tag de este repositorio se compila bajo demanda:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Coordenadas: `com.github.AnderCMD.Autto-Framework:autto-core:<tag>` (p. ej. `v1.2.0`), compilado para Java 21+.

**Maven Central.** Subir un tag ejecuta el workflow *Release*, que publica `io.github.andercmd:autto-core` en Maven
Central si existen los secretos `MAVEN_CENTRAL_*` / `GPG_*` (perfil `release`); cada release además se firma y se
atesta, ver [Pruebas avanzadas](pruebas-avanzadas.md#controles-de-calidad-y-cadena-de-suministro).

**Repositorio interno (Nexus, Artifactory, GitHub Packages, Azure Artifacts).** Recomendado para empresas: controlas
la disponibilidad y puedes analizar el artefacto.

```bash
./mvnw deploy -pl autto-core -am -DskipTests \
    -DaltDeploymentRepository=empresa::https://nexus.empresa.com/repository/maven-releases/
```

### 2. `pom.xml` mínimo

Importa el BOM de Autto para que Selenium, Cucumber, Spring Boot y JUnit usen las versiones con las que se probó el
motor:

```xml
<properties>
    <autto.version>v1.2.0</autto.version>   <!-- io.github.andercmd:1.2.0 en un repositorio interno -->
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.github.AnderCMD.Autto-Framework</groupId>
            <artifactId>autto-parent</artifactId>
            <version>${autto.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.github.AnderCMD.Autto-Framework</groupId>
        <artifactId>autto-core</artifactId>
        <version>${autto.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency><groupId>io.cucumber</groupId><artifactId>cucumber-junit-platform-engine</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.junit.platform</groupId><artifactId>junit-platform-suite</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework</groupId><artifactId>spring-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

Configura Surefire como en `autto-e2e/pom.xml` (incluye `**/*Suite.java` y excluye el motor `cucumber` del
descubrimiento directo).

### 3. Copia el esqueleto de `autto-e2e`

| Archivo | Para qué |
|---|---|
| `CucumberTestSuite.java` | Punto de entrada (suite de JUnit Platform) |
| `CucumberSpringConfiguration.java` | Puente Cucumber ↔ Spring |
| `E2eTestApplication.java` | Configuración Spring de tu suite (renómbrala) |
| `junit-platform.properties` | Glue (`<tu.paquete>,io.github.andercmd.autto.core.cucumber`), plugins, tags, paralelismo |
| `application.yml` + perfiles | Ajustes `autto.*` y usuarios de prueba |
| `logback-test.xml` | Logs con enmascarado de secretos |
| `.env.example`, `.gitignore` | Nombres de los secretos; nunca subas `.env` |

Después añade tus carpetas `features/<funcionalidad>/`. No hace falta copiar nada más del motor.

## Actualizar

- Lee el [CHANGELOG](../../CHANGELOG.md): Autto sigue Semantic Versioning, así que las versiones menores nunca rompen
  claves de configuración ni APIs públicas.
- Si usas la plantilla: fusiona el tag del repositorio original en el tuyo. Si usas el motor: sube `autto.version`.
- Ejecuta `@smoke` contra un entorno antes de llevar la nueva versión a todos los pipelines.
