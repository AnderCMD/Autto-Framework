# CI/CD

[← Volver al README](../../README.es.md) · [English](../en/ci-cd.md)

La suite es un build de Maven normal, así que funciona en cualquier CI. Puntos clave:

- Usa `./mvnw` (no hace falta instalar Maven).
- Ejecuta en headless: `-Dbrowser.headless=true`.
- Publica siempre `target/autto-reports/` como artefacto, también cuando el build falla.
- Pasa los secretos como variables de entorno (`AUTTO_*` o nombres usados en `${placeholders}`).

## GitHub Actions (incluido)

| Workflow | Disparador | Qué hace |
|---|---|---|
| `.github/workflows/ci.yml` | push a `main`, pull requests, manual | Tests unitarios + chequeo del reporte sin navegador; después `@smoke` en Windows, macOS y Linux × Chrome, Firefox, Edge (+ Safari en macOS). |
| `.github/workflows/nightly.yml` | cada noche, manual | Levanta el Selenium Grid de Docker y ejecuta `@regression` en paralelo para cada navegador. |

JDK 27 se instala con `oracle-actions/setup-java` desde jdk.java.net (builds Early Access hasta la GA de marzo de
2027). Para usar otra versión cambia `JAVA_RELEASE` en el workflow y pasa `-Djava.version=<n>`.

Las ejecuciones manuales aceptan una expresión de tags (*Actions → CI → Run workflow*).

## Jenkins

```groovy
pipeline {
    agent any
    tools { jdk 'jdk-27' }
    parameters {
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge'])
        string(name: 'TAGS', defaultValue: '@smoke')
    }
    stages {
        stage('Test') {
            steps {
                sh "./mvnw -B test -Dbrowser=${params.BROWSER} -Dbrowser.headless=true -Dcucumber.filter.tags='${params.TAGS}' -Dautto.ignoreFailures=true"
            }
        }
    }
    post {
        always {
            junit 'target/autto-reports/cucumber/cucumber-junit.xml'
            publishHTML(target: [reportDir: 'target/autto-reports', reportFiles: 'index.html',
                                 reportName: 'Autto report', keepAll: true, alwaysLinkToLastBuild: true])
            archiveArtifacts artifacts: 'target/autto-reports/**', allowEmptyArchive: true
        }
    }
}
```

> La Content Security Policy por defecto de Jenkins bloquea los scripts del reporte. Relájala para el plugin HTML
> Publisher o descarga la carpeta archivada.

## GitLab CI

```yaml
e2e:
  image: maven:3-eclipse-temurin-25   # usa una imagen con JDK 27 cuando exista y quita -Djava.version
  services:
    - name: selenium/standalone-chrome:latest
      alias: selenium
  variables:
    AUTTO_EXECUTION_TARGET: remote
    AUTTO_REMOTE_URL: http://selenium:4444
  script:
    - ./mvnw -B test -Djava.version=25 -Dcucumber.filter.tags="@smoke"
  artifacts:
    when: always
    paths: [target/autto-reports/]
    reports:
      junit: target/autto-reports/cucumber/cucumber-junit.xml
```

## Azure DevOps

```yaml
steps:
  - script: ./mvnw -B test -Dbrowser.headless=true -Dautto.ignoreFailures=true
  - task: PublishTestResults@2
    condition: always()
    inputs:
      testResultsFiles: target/autto-reports/cucumber/cucumber-junit.xml
  - task: PublishPipelineArtifact@1
    condition: always()
    inputs:
      targetPath: target/autto-reports
      artifact: autto-report
```

## Publicar el reporte en GitHub Pages

Añade un job después de las pruebas que suba `target/autto-reports` con `actions/upload-pages-artifact` y lo
despliegue con `actions/deploy-pages`. El reporte es HTML estático y funciona tal cual.
