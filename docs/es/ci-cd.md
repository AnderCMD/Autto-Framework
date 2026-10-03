# CI/CD

[← Volver al README](../../README.es.md) · [English](../en/ci-cd.md)

Puntos clave para cualquier CI:

- Usa `./mvnw` (no hace falta instalar Maven) y `SPRING_PROFILES_ACTIVE=<entorno>,ci` (headless).
- Proporciona los secretos como **secretos del pipeline** con los mismos nombres que en `.env.example`.
- Publica **siempre** `autto-e2e/target/autto-reports/` como artefacto, también cuando el build falla.

## GitHub Actions (incluido)

| Workflow | Disparador | Jobs |
|---|---|---|
| `ci.yml` | push a `main`, pull requests, manual | **Secret scanning** (gitleaks, todo el historial) · **Dependency review** (pull requests: fallan las dependencias nuevas con vulnerabilidades altas/críticas) · **Build** (enforcer, checkstyle, tests unitarios, escenarios sin navegador) · **Matriz E2E** Windows/macOS/Linux × Chrome/Firefox/Edge (+ Safari) |
| `nightly.yml` | cada noche, manual | `@regression` contra el Selenium Grid de Docker (en paralelo) y con navegadores Docker de WebDriverManager |

Secreto requerido: `SAUCE_PASSWORD` (*Settings → Secrets and variables → Actions*). El workflow demo usa como
respaldo la contraseña pública de la demo; elimina ese respaldo en una aplicación real.

JDK 27 se instala con `oracle-actions/setup-java` desde jdk.java.net (builds Early Access hasta la GA de marzo de
2027). Para otro JDK cambia `JAVA_RELEASE` y añade `-Djava.version=<n>` a los comandos Maven.

## Jenkins

```groovy
pipeline {
    agent any
    tools { jdk 'jdk-27' }
    parameters {
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge'])
        choice(name: 'ENV', choices: ['qa', 'staging'])
        string(name: 'TAGS', defaultValue: '@smoke')
    }
    environment {
        SPRING_PROFILES_ACTIVE = "${params.ENV},ci"
        AUTTO_BROWSER_NAME = "${params.BROWSER}"
    }
    stages {
        stage('Test') {
            steps {
                withCredentials([string(credentialsId: 'sauce-password', variable: 'SAUCE_PASSWORD')]) {
                    sh "./mvnw -B install -Dcucumber.filter.tags='${params.TAGS}' -Dautto.ignoreFailures=true"
                }
            }
        }
    }
    post {
        always {
            junit 'autto-e2e/target/autto-reports/cucumber/cucumber-junit.xml'
            publishHTML(target: [reportDir: 'autto-e2e/target/autto-reports', reportFiles: 'index.html',
                                 reportName: 'Autto report', keepAll: true, alwaysLinkToLastBuild: true])
            archiveArtifacts artifacts: 'autto-e2e/target/autto-reports/**', allowEmptyArchive: true
        }
    }
}
```

> La Content Security Policy por defecto de Jenkins bloquea los scripts del reporte; relájala para HTML Publisher o
> descarga la carpeta archivada.

## GitLab CI

```yaml
e2e:
  image: maven:3-eclipse-temurin-25          # cambia a una imagen con JDK 27 cuando exista y quita -Djava.version
  services:
    - name: selenium/standalone-chrome:latest
      alias: selenium
  variables:
    SPRING_PROFILES_ACTIVE: qa,ci,grid
    SELENIUM_GRID_URL: http://selenium:4444
    # SAUCE_PASSWORD: definida en Settings → CI/CD → Variables (masked, protected)
  script:
    - ./mvnw -B install -Djava.version=25 -Dcucumber.filter.tags="@smoke"
  artifacts:
    when: always
    paths: [autto-e2e/target/autto-reports/]
    reports:
      junit: autto-e2e/target/autto-reports/cucumber/cucumber-junit.xml
```

## Azure DevOps

```yaml
variables:
  - group: autto-secrets            # contiene SAUCE_PASSWORD (o enlázalo a Azure Key Vault)
steps:
  - script: ./mvnw -B install -Dautto.ignoreFailures=true
    env:
      SPRING_PROFILES_ACTIVE: qa,ci
      SAUCE_PASSWORD: $(SAUCE_PASSWORD)
  - task: PublishTestResults@2
    condition: always()
    inputs:
      testResultsFiles: autto-e2e/target/autto-reports/cucumber/cucumber-junit.xml
  - task: PublishPipelineArtifact@1
    condition: always()
    inputs:
      targetPath: autto-e2e/target/autto-reports
      artifact: autto-report
```

## Publicar `autto-core` para otros equipos

`autto-core` es un artefacto Maven normal. Añade una sección `distributionManagement` (Nexus, Artifactory, GitHub
Packages) al POM padre y ejecuta `./mvnw -pl autto-core deploy`. Los equipos de producto dependen de
`io.github.andercmd:autto-core:<versión>` y solo mantienen su módulo `*-e2e`.
