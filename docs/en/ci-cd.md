# CI/CD

[← Back to README](../../README.md) · [Español](../es/ci-cd.md)

The suite is a plain Maven build, so it runs in any CI. Key points:

- Use `./mvnw` (no Maven installation needed).
- Run headless: `-Dbrowser.headless=true`.
- Always publish `target/autto-reports/` as an artifact, also when the build fails.
- Pass secrets as environment variables (`AUTTO_*` or names referenced by `${placeholders}`).

## GitHub Actions (included)

| Workflow | Trigger | What it does |
|---|---|---|
| `.github/workflows/ci.yml` | push to `main`, pull requests, manual | Unit tests + browser-less report check, then `@smoke` on Windows, macOS and Linux × Chrome, Firefox, Edge (+ Safari on macOS). |
| `.github/workflows/nightly.yml` | every night, manual | Starts the Docker Selenium Grid and runs `@regression` in parallel for each browser. |

JDK 27 is installed with `oracle-actions/setup-java` from jdk.java.net (Early Access builds until the March 2027 GA).
To use another version change `JAVA_RELEASE` in the workflow and pass `-Djava.version=<n>`.

Manual runs accept a tag expression (*Actions → CI → Run workflow*).

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

> Jenkins' default Content Security Policy blocks the report's scripts. Relax it for the HTML Publisher plugin or
> download the archived folder.

## GitLab CI

```yaml
e2e:
  image: maven:3-eclipse-temurin-25   # use a JDK 27 image when available, and drop -Djava.version
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

## Publishing the report on GitHub Pages

Add a job after the tests that uploads `target/autto-reports` with `actions/upload-pages-artifact` and deploys it
with `actions/deploy-pages`. The report is static HTML and works as-is.
