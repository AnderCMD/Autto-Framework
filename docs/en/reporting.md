# Reports & evidence

[← Back to README](../../README.md) · [Español](../es/reportes.md)

## Outputs

After every run `autto-e2e/target/autto-reports/` contains (folder configurable with `autto.report.dir`):

```
target/autto-reports/
├── index.html              Extent Spark report — open this one
├── failed.html             Only failed / warning scenarios
├── extent.json             Raw Extent data (archive, custom dashboards)
├── spark/                  Report assets (offline mode)
├── screenshots/*.png
├── videos/*.mp4
├── attachments/*           Page sources, CSV, PDF and any other attached file
├── logs/autto.log          Execution log with thread and scenario name
├── rerun.txt               Failed scenarios (re-run with -Dcucumber.features=@target/autto-reports/rerun.txt)
└── cucumber/
    ├── cucumber.html       Native Cucumber report (single file)
    ├── cucumber.json       For Jenkins Cucumber Reports, Xray, Zephyr…
    ├── cucumber-junit.xml  For any CI test tab
    └── cucumber.ndjson     Cucumber Messages
```

The folder is self-contained: zip it or publish it as a CI artifact and it opens anywhere, even offline.

## The Extent report

| View | What you get |
|---|---|
| **Dashboard** | Start/end time, features/scenarios/steps charts, timeline, tags, devices, environment table. |
| **Tests** | Feature → Scenario (or Scenario Outline → examples) → Gherkin steps with keyword, status, duration, data tables, doc strings, logs, screenshots, videos and stack traces. |
| **Tags** | Statistics per Cucumber tag (`@smoke`, `@checkout`…). |
| **Devices** | Statistics per browser/device (`chrome-141.0-windows`), useful in cross-browser runs. |
| **Authors** | Statistics per `@author:<name>` tag. |
| **Exceptions** | Every exception type with the scenarios that raised it. |

### Where evidence appears

| Evidence | Location in the report |
|---|---|
| Screenshot at failure (`autto.evidence.screenshot=on-failure`) | Under the failing step |
| Screenshot after each step (`autto.evidence.screenshot=always`) | Under every step |
| `Report.*` calls and `scenario.log/attach` inside a step | Under that step |
| URL, page source, browser console, video | In the **Evidence** node at the end of the scenario |
| Exceptions in hooks | In the **Setup** or **Evidence** node |

### Videos

Videos are recorded by periodically capturing WebDriver screenshots and encoding them to H.264/MP4 with JCodec
(pure Java). This works for headless browsers, Selenium Grid, cloud vendors and Appium without ffmpeg or a
desktop session.

- `autto.evidence.video=on-failure` (default): every scenario is recorded, only failures are kept.
- `autto.evidence.video-fps` controls smoothness vs. overhead (default 3).
- Only the last `autto.evidence.video-max-duration` is kept.
- The MP4 is played inline in Chrome, Edge, Firefox and Safari. Open-source Chromium builds lack the H.264 codec;
  use the *Download video* link there.
- While recording, `autto.browser.unhandled-prompt` defaults to `ignore` so background screenshots never dismiss alerts.

Selenium Grid users can also enable the Grid's own recording with the capability `se:recordVideo: true`.

## Report API

`io.github.andercmd.autto.core.report.Report` writes into the current step (thread-safe, no-op outside a scenario,
registered secrets are masked):

```java
Report.info("Order created: " + id);
Report.pass("Payment accepted");             // green label
Report.warning("Slow response: 4.2 s");      // marks the scenario as Warning in Extent
Report.fail("Soft failure, keep going");     // marks the step as failed without throwing
Report.screenshot("Cart before checkout");
Report.table("Order", Map.of("id", id, "total", total));
Report.table("Rows", List.of(List.of("a", "b"), List.of("c", "d")));
Report.json("Response", jsonBody);
Report.code("SQL", query);
Report.file("Invoice", pdfBytes, "pdf");
Report.video("Custom recording", mp4Bytes);
Report.author("jane.doe");
Report.html("<b>trusted HTML</b>");
```

Cucumber's own API also works and reaches every report (Extent, Cucumber HTML, JSON):

```java
scenario.log("text");
scenario.attach(pngBytes, "image/png", "Screenshot");
scenario.attach(json.getBytes(UTF_8), "application/json", "payload");
scenario.attach(pdf, "application/pdf", "invoice.pdf");   // stored in attachments/ and linked
```

## Customization

- **Theme, title, name:** `autto.report.theme`, `autto.report.title`, `autto.report.name`.
- **Extra dashboard rows:** `autto.report.info.Release: 2.4.0`, `autto.report.info.Team: Payments`.
- **Styles and scripts:** edit `autto-core/src/main/resources/autto/report/autto.css` and `autto.js`.
- **History:** `autto.report.timestamped=true` creates one folder per run.
- **Single portable file:** `autto.report.screenshots-base64=true` embeds screenshots in the HTML (videos stay files).

The dashboard environment table shows profiles, browser, driver resolution, target, OS, Java, Selenium, Cucumber and
Spring Boot versions and the CI run. Secret values never appear: URLs are redacted and secrets masked.

## Logs

`logs/autto.log` contains every log line with thread and scenario name (`%X{scenario}`), which makes parallel runs
readable, and secrets are masked (`%maskedMsg`). Use `-Dautto.log.level=DEBUG` to trace each interaction of
`BasePage`.
