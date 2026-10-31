# CI/CD

[← Back to README](../../README.md) · [Español](../es/ci-cd.md)

Key points for any CI:

- Use `./mvnw` (no Maven installation needed) and `SPRING_PROFILES_ACTIVE=<env>,ci` (headless).
- Provide secrets as **pipeline secrets** with the same names as in `.env.example`.
- Publish `autto-e2e/target/autto-reports/` as an artifact **always**, also when the build fails.

## GitHub Actions (included)

| Workflow | Trigger | Jobs |
|---|---|---|
| `ci.yml` | push to `main`, pull requests, manual | **Secret scanning** (gitleaks, full history) · **Dependency review** (pull requests: new dependencies with high/critical vulnerabilities fail) · **Build** on JDK 21, 25 and 27 (enforcer, checkstyle, unit tests, coverage gate, browser-less scenarios; SpotBugs on 21; 27 is experimental) · **E2E**: pull requests run Linux + Chrome only, `main` and manual runs the Windows/macOS/Linux × Chrome/Firefox/Edge (+ Safari) matrix. Failed scenarios are re-run once |
| `nightly.yml` | nightly, manual | **OSV** dependency vulnerabilities · `@regression` against the Docker Selenium Grid (parallel) and with WebDriverManager Docker browsers, failures re-run once · optional **Pages publication** of every report with history · optional Slack / Teams summary |
| `release.yml` | tag `vX.Y.Z` | Build, tests, **SBOM**, build-provenance **attestation**, keyless **Sigstore** signatures, GitHub release, and **Maven Central** when its secrets exist |

Required secret: `SAUCE_PASSWORD` (*Settings → Secrets and variables → Actions*). The demo workflow falls back to
the public demo password; remove that fallback for a real application.

Optional configuration:

| Name | Kind | Effect |
|---|---|---|
| `AUTTO_NOTIFICATIONS_WEBHOOK_URL` | secret | Slack / Teams incoming webhook: the nightly summary is sent when something fails (`AUTTO_NOTIFICATIONS_TYPE` variable: `slack` or `teams`) |
| `PUBLISH_REPORTS` | variable = `true` | Publishes each nightly report to the `gh-pages` branch (`runs/<n>/`) with a history page. Enable *Settings → Pages → Deploy from a branch → gh-pages*. Reports are public when the repository is public |
| `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` | secrets | Enable publication to Maven Central from the Release workflow |

Every action is pinned to a commit SHA (the comment keeps the version); Dependabot opens the update pull requests.
JDK 21 and 25 come from `actions/setup-java` (Temurin); JDK 27 from `oracle-actions/setup-java` (jdk.java.net, Early
Access until the March 2027 GA). The e2e jobs use `JAVA_RELEASE`; to use another JDK change it and add
`-Djava.version=<n>` to the Maven commands.

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
