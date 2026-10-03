@api @nobrowser @regression
Feature: Storefront availability through HTTP
  API checks run without a browser: they are fast, stable and can also prepare data for UI scenarios.
  Every request and response is attached to the report with secrets masked.

  @smoke
  Scenario: The storefront home page is online
    When the client requests "/"
    Then the response status is 200
    And the response is an HTML page titled "Swag Labs"

  Scenario: Unknown resources are not found
    When the client requests "/this-page-does-not-exist"
    Then the response status is 404
