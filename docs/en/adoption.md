# Adoption guide

[← Back to README](../../README.md) · [Español](../es/adopcion.md)

Autto scales from a single tester automating one web application to dozens of teams sharing one engine. Start at
the level you need today; moving up never requires a rewrite.

## Pick your level

| | **Starter** | **Team** | **Enterprise** |
|---|---|---|---|
| Who | 1–2 people, one application | One product team | Many teams and products |
| How to start | *Use this template* / clone | Template + CI | Own `<product>-e2e` projects depending on `autto-core` |
| Environments | `qa` profile | `qa`, `staging`, `ci` profiles | One profile per environment and execution target |
| Execution | Local Chrome | CI matrix, parallel scenarios | Selenium Grid / cloud vendor, nightly regressions |
| Test types | UI | UI + API data set-up | UI + API + accessibility + mobile |
| Secrets | `.env` | `.env` + CI secrets | Secret manager (Vault, Azure Key Vault, AWS Secrets Manager) |
| Engine updates | — | Merge upstream releases | Bump the `autto-core` version |

### Starter — first scenario in 15 minutes

1. Click **Use this template** on GitHub (or clone the repository), **or** generate a lean standalone project:
   `./scripts/new-project.sh ../my-e2e com.acme my-e2e` (see [Advanced testing](advanced-testing.md#start-a-new-project)).
   Open the repository in a dev container / Codespace for a ready environment.
2. Follow [Getting started](getting-started.md) and run the demo once.
3. Point `autto.base-url` to your application, delete `features/*` of the demo and write your first feature
   ([Writing tests](writing-tests.md)).
4. Keep the defaults: local Chrome, videos and screenshots on failure, Extent report.

### Team — CI, environments and speed

- One `application-<env>.yml` per environment; secrets as CI secrets with the names of `.env.example`.
- Enable the included GitHub Actions workflows (or copy the Jenkins / GitLab / Azure DevOps examples in
  [CI/CD](ci-cd.md)).
- Tag scenarios (`@smoke`, `@regression`, feature tags) and run `@smoke` on every pull request.
- Turn on parallel execution (`cucumber.execution.parallel.enabled=true`) once scenarios are independent.
- Create test data through APIs ([API testing](api-testing.md)) instead of the UI.
- Assign owners per feature folder in `CODEOWNERS`.

### Enterprise — one engine, many suites

Publish `autto-core` once and let each product team own a small suite project. Engine fixes and upgrades reach every
team by bumping one version, and teams never fork the engine.

```
company/autto-core           (this repository or your fork)  →  published to JitPack / Nexus / Artifactory
company/checkout-e2e         depends on autto-core
company/backoffice-e2e       depends on autto-core
company/mobile-app-e2e       depends on autto-core (execution.target=appium)
```

Recommended additions: Selenium Grid or a cloud vendor (`grid` / `browserstack` profiles), a secret manager, nightly
`@regression` runs, accessibility audits on key pages, and a shared library of API beans for test data.

## Using `autto-core` from your own project

### 1. Get the artifact

**JitPack (zero setup, public).** Every tag of this repository is built on demand:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Coordinates: `com.github.AnderCMD.Autto-Framework:autto-core:<tag>` (e.g. `v1.2.0`), compiled for Java 21+.

**Maven Central.** Pushing a tag runs the *Release* workflow, which publishes `io.github.andercmd:autto-core` to Maven
Central when the `MAVEN_CENTRAL_*` / `GPG_*` secrets exist (profile `release`); every release is also signed and
attested, see [Advanced testing](advanced-testing.md#supply-chain-quality-gates).

**Internal repository (Nexus, Artifactory, GitHub Packages, Azure Artifacts).** Recommended for companies: you
control availability and can scan the artifact.

```bash
./mvnw deploy -pl autto-core -am -DskipTests \
    -DaltDeploymentRepository=company::https://nexus.company.com/repository/maven-releases/
```

### 2. Minimal `pom.xml`

Import the Autto BOM so Selenium, Cucumber, Spring Boot and JUnit get the versions the engine was tested with:

```xml
<properties>
    <autto.version>v1.2.0</autto.version>   <!-- io.github.andercmd:1.2.0 in an internal repository -->
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

Configure Surefire as in `autto-e2e/pom.xml` (include `**/*Suite.java`, exclude the `cucumber` engine from direct
discovery).

### 3. Copy the skeleton from `autto-e2e`

| File | Purpose |
|---|---|
| `CucumberTestSuite.java` | Entry point (JUnit Platform suite) |
| `CucumberSpringConfiguration.java` | Cucumber ↔ Spring bridge |
| `E2eTestApplication.java` | Spring configuration of your suite (rename it) |
| `junit-platform.properties` | Glue (`<your.package>,io.github.andercmd.autto.core.cucumber`), plugins, tags, parallelism |
| `application.yml` + profiles | `autto.*` settings and test users |
| `logback-test.xml` | Logging with secret masking |
| `.env.example`, `.gitignore` | Secret names; never commit `.env` |

Then add your `features/<feature>/` folders. Nothing else from the engine needs to be copied.

## Upgrading

- Read the [CHANGELOG](../../CHANGELOG.md): Autto follows Semantic Versioning, so minor versions never break
  configuration keys or public APIs.
- Template users: merge the upstream tag into your repository. Engine users: bump `autto.version`.
- Run `@smoke` against one environment before rolling the new version out to every pipeline.
