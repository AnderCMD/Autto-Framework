# Getting started

[← Back to README](../../README.md) · [Español](../es/primeros-pasos.md)

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 27 (or ≥ 21 with `-Djava.version=<n>`) | Java 27 GA is planned for March 2027. Until then use an [Early Access build](https://jdk.java.net/27/). |
| Maven | not required | The included Maven Wrapper (`./mvnw`, `mvnw.cmd`) downloads Maven 3.9.16. |
| Browser | Chrome, Firefox, Edge or Safari | Selenium Manager resolves drivers automatically and can download Chrome/Firefox if missing. |
| Docker | optional | Only to run the local Selenium Grid (`docker compose up`). |
| Appium 2+ | optional | Only for mobile (`execution.target=appium`). |

Check your setup:

```bash
java -version
./mvnw -v          # Windows: mvnw.cmd -v
```

## First run

```bash
# 1. Framework unit tests (no browser, a few seconds)
./mvnw -Punit test

# 2. Report pipeline without a browser
./mvnw test -Dcucumber.filter.tags=@showcase

# 3. Full suite with Chrome
./mvnw test
```

Reports are written to `target/autto-reports/`:

| File | Content |
|---|---|
| `index.html` | Extent Spark report (open this one) |
| `failed.html` | Same report, failures and warnings only |
| `cucumber/cucumber.html` | Native Cucumber HTML report |
| `cucumber/cucumber.json`, `cucumber-junit.xml`, `cucumber.ndjson` | Machine readable outputs for CI tools |
| `rerun.txt` | Failed scenarios, to re-run them |
| `logs/autto.log` | Full execution log |

## Building with a JDK older than 27

The `java.version` property controls `--release` and the Maven Enforcer rule:

```bash
./mvnw test -Djava.version=25
```

To make it permanent for your machine, add it to `.mvn/maven.config` (create the file):

```
-Djava.version=25
```

## IDE setup

### IntelliJ IDEA

1. *File → Open* and select `pom.xml` (open as project).
2. Install the **Cucumber for Java** and **Gherkin** plugins.
3. Run `CucumberTestSuite` or right-click any `.feature` file → *Run*.
   For feature-file runs, set the glue to `io.github.andercmd.autto` in the run configuration template.

### VS Code

1. Install *Extension Pack for Java* and *Cucumber (Gherkin) Full Support*.
2. Add to `.vscode/settings.json`:

```json
{
  "cucumberautocomplete.steps": ["src/test/java/**/*.java"],
  "cucumberautocomplete.syncfeatures": "src/test/resources/features/**/*.feature"
}
```

### Eclipse

Import as *Existing Maven Project* and install *Cucumber Eclipse Plugin* from the marketplace.

## Make it yours

1. Rename the package `io.github.andercmd.autto` (IDE refactor) and update `groupId` / `artifactId` in `pom.xml`
   and `cucumber.glue` in `junit-platform.properties`.
2. Set `base.url` in `src/test/resources/environments/*.properties`.
3. Delete the demo folders under `features/` (keep `showcase` if you want a browser-less health check).
4. Create your first feature: see [Writing tests](writing-tests.md).
