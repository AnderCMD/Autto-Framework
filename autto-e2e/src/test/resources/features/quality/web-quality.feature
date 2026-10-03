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

  @network
  Scenario: A blocked third party does not break the login
    Given the third party "backtrace.io" is blocked
    And the customer is on the login page
    When the customer logs in as "standard"
    Then the products catalog is displayed

  @network
  Scenario: A stubbed backend response replaces the real one
    Given the response of "www.saucedemo.com" is stubbed with a page titled "Stubbed by Autto"
    When the browser opens "https://www.saucedemo.com/"
    Then the browser title is "Stubbed by Autto"

  @network
  Scenario: A delayed request still reaches the real server
    Given requests to "www.saucedemo.com" are delayed by 2 seconds
    When the browser opens "https://www.saucedemo.com/"
    Then the browser title is "Swag Labs"
    And opening it took at least 2 seconds

  # Baselines depend on browser, version and OS: create them with -Dautto.visual.update=true, review and commit
  # them, then remove @ignore. Run in Docker (-Dspring.profiles.active=qa,docker) for reproducible pixels.
  @visual @ignore
  Scenario: The login page looks as approved
    Given the customer is on the login page
    Then the page looks like the "login-page" baseline
