@accessibility @regression
Feature: Accessibility
  Pages are audited with axe-core against WCAG 2.1 A/AA (autto.accessibility.tags). Violations of the configured
  impact (autto.accessibility.fail-on) or worse fail the scenario; all of them are listed in the report.

  Scenario: The login page has no critical accessibility violations
    Given the customer is on the login page
    Then the page has no critical accessibility violations
