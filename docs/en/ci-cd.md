# CI/CD

[← Back to README](../../README.md) · [Español](../es/ci-cd.md)

Key points for any CI:

- Use `./mvnw` (no Maven installation needed) and `SPRING_PROFILES_ACTIVE=<env>,ci` (headless).
- Provide secrets as **pipeline secrets** with the same names as in `.env.example`.
- Publish `autto-e2e/target/autto-reports/` as an artifact **always**, also when the build fails.

## GitHub Actions (included)

| Workflow | Trigger | Jobs |
|---|---|---|
| `ci.yml` | push to `main`, pull requests, manual | **Secret scanning** (gitleaks, full history) · **Dependency review** (pull requests: new dependencies with high/critical vulnerabilities fail) · **Build** (enforcer, checkstyle, unit tests, browser-less scenarios) · **E2E matrix** Windows/macOS/Linux × Chrome/Firefox/Edge (+ Safari) |
| `nightly.yml` | nightly, manual | `@regression` against the Docker Selenium Grid (parallel) and with WebDriverManager Docker browsers |

Required secret: `SAUCE_PASSWORD` (*Settings → Secrets and variables → Actions*). The demo workflow falls back to
the public demo password; remove that fallback for a real application.

JDK 27 is installed with `oracle-actions/setup-java` from jdk.java.net (Early Access builds until the March 2027 GA).
To use another JDK change `JAVA_RELEASE` and add `-Djava.version=<n>` to the Maven commands.

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

> Jenkins' default Content Security Policy blocks the report's scripts; relax it for HTML Publisher or download the
> archived folder.

## GitLab CI

```yaml
e2e:
  image: maven:3-eclipse-temurin-25          # switch to a JDK 27 image when available and drop -Djava.version
  services:
    - name: selenium/standalone-chrome:latest
      alias: selenium
  variables:
    SPRING_PROFILES_ACTIVE: qa,ci,grid
    SELENIUM_GRID_URL: http://selenium:4444
    # SAUCE_PASSWORD: defined in Settings → CI/CD → Variables (masked, protected)
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
  - group: autto-secrets            # contains SAUCE_PASSWORD (or link it to Azure Key Vault)
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

## Publishing `autto-core` for other teams

`autto-core` is a normal Maven artifact. Add a `distributionManagement` section (Nexus, Artifactory, GitHub Packages)
to the parent POM and run `./mvnw -pl autto-core deploy`. Product teams then depend on
`io.github.andercmd:autto-core:<version>` and keep only their `*-e2e` module.
