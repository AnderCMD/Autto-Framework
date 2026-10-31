@mobile @ignore
Feature: Native mobile app
  Same BDD flow, same report and evidence, on a real or emulated device through Appium. Remove @ignore, start an
  Appium server and an emulator, and run with -Dspring.profiles.active=qa,appium (see application-appium.yml).

  Scenario: The Settings app opens on the device
    Given the Settings app is open
    Then the Settings search bar is displayed
