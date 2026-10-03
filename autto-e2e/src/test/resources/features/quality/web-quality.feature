@quality @regression
Feature: Web quality
  Cross-cutting checks that run on a real browser: performance budgets, responsive layouts, resilience to
  third-party failures and visual regression.

  @performance
  Scenario: The login page respects the performance budget
    Given the customer is on the login page
    Then the page is within the performance budget

  @viewport:390x844
  Scenario: The login page works on a phone-sized screen
    Given the customer is on the login page
    When the customer logs in as "standard"
    Then the products catalog is displayed

  # Experimental: network interception uses WebDriver BiDi, whose request interception is not yet reliable in every
  # Chromium release (verified failing on Chrome 150 with "Invalid InterceptionId"). Remove @ignore to try it.
  @network @ignore
  Scenario: A blocked third party does not break the login
    Given the third party "backtrace.io" is blocked
    And the customer is on the login page
    When the customer logs in as "standard"
    Then the products catalog is displayed

  # Baselines depend on browser, version and OS: create them with -Dautto.visual.update=true, review and commit
  # them, then remove @ignore. Run in Docker (-Dspring.profiles.active=qa,docker) for reproducible pixels.
  @visual @ignore
  Scenario: The login page looks as approved
    Given the customer is on the login page
    Then the page looks like the "login-page" baseline
