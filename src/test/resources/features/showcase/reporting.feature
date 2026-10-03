@showcase @nobrowser
Feature: Report showcase
  Demonstrates everything the Extent report can render, without needing a browser.
  It also works as a fast health check of the reporting pipeline: mvn test -Dcucumber.filter.tags=@showcase

  Scenario: Rich content in the report
    Given a data table with the supported browsers:
      | browser | engine   | headless |
      | chrome  | Blink    | yes      |
      | firefox | Gecko    | yes      |
      | edge    | Blink    | yes      |
      | safari  | WebKit   | no       |
    And the following JSON payload:
      """json
      {
        "framework": "Autto",
        "reports": ["Extent Spark", "Cucumber HTML", "JUnit XML", "Cucumber JSON"],
        "evidence": {"screenshots": true, "videos": true}
      }
      """
    When the step writes logs, tables and attachments
    Then the report contains them
