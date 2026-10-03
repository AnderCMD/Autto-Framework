# Getting started

[← Back to README](../../README.md) · [Español](../es/primeros-pasos.md)

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 27 (or ≥ 21 with `-Djava.version=<n>`) | Java 27 GA is planned for March 2027. Until then use an [Early Access build](https://jdk.java.net/27/). |
| Maven | not required | The Maven Wrapper (`./mvnw`, `mvnw.cmd`) downloads Maven 3.9.16. |
| Browser | any of Chrome, Chromium, Firefox, Edge, Safari | Drivers are resolved automatically (WebDriverManager → Selenium Manager). |
| Docker | optional | Browsers in containers (`docker` profile) or local Selenium Grid. |
| Appium 2+ | optional | Mobile (`autto.execution.target=appium`). |

## First run

```bash
git clone https://github.com/AnderCMD/Autto-Framework.git
cd Autto-Framework

# 1. Secrets: create your local, git-ignored .env
cp .env.example .env                       # Windows: copy .env.example .env
#    and set SAUCE_PASSWORD=secret_sauce (public password of the demo store)

# 2. Build + framework unit tests + browser-less report check
./mvnw verify -Dcucumber.filter.tags=@showcase

# 3. Full suite with Chrome
./mvnw install
```

Open **`autto-e2e/target/autto-reports/index.html`**.

> `./mvnw install` builds `autto-core` and then runs the suite. To run only the suite once the core is installed:
> `./mvnw -pl autto-e2e test`.

## Building with a JDK older than 27

```bash
./mvnw install -Djava.version=25
```

Make it permanent for your machine in `.mvn/maven.config` (do not commit it if your team uses JDK 27):

```
-Djava.version=25
```

## IDE setup

### IntelliJ IDEA

1. *File → Open* → the root `pom.xml` (open as project).
2. Plugins: **Cucumber for Java**, **Gherkin**, **Spring** (Ultimate) or **Spring Boot Assistant** (Community) for
   `application.yml` auto-completion.
3. *Settings → Editor → Code Style → Java*: import `.editorconfig` (already applied automatically).
4. Run `CucumberTestSuite`, or right-click a `.feature` → *Run* (set the glue to
   `io.github.andercmd.autto.e2e io.github.andercmd.autto.core.cucumber` in the Cucumber run template).
5. IntelliJ reads `.env` through the framework itself, no plugin needed.

### VS Code

Extensions: *Extension Pack for Java*, *Spring Boot Extension Pack*, *Cucumber (Gherkin) Full Support*.

```json
{
  "cucumberautocomplete.steps": ["autto-e2e/src/test/java/**/*.java"],
  "cucumberautocomplete.syncfeatures": "autto-e2e/src/test/resources/features/**/*.feature"
}
```

## Make it yours

1. Rename packages `io.github.andercmd.autto` and the `groupId` in the POMs. Update `cucumber.glue`.
2. Rename `autto-e2e` to your application (`shop-e2e`) and set `autto.base-url` in `application-<env>.yml`.
3. Replace the demo `features/*` folders and `test-data.users` with your own.
4. Put your secrets in `.env.example` (names only) and in your CI secret store.
5. Optional: enable `pre-commit install` for local secret scanning.
