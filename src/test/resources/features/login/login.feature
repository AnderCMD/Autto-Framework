@login @regression
Feature: Login
  As a registered customer of the store
  I want to sign in with my credentials
  So that I can browse and buy products

  Background:
    Given the customer is on the login page

  @smoke @author:AnderCMD
  Scenario: Successful login with a standard user
    When the customer logs in as "standard"
    Then the products catalog is displayed

  Scenario Outline: Login is rejected with invalid credentials
    When the customer logs in with username "<username>" and password "<password>"
    Then the login error "<message>" is shown

    Examples:
      | username        | password     | message                                                                   |
      | locked_out_user | secret_sauce | Epic sadface: Sorry, this user has been locked out.                       |
      | standard_user   | wrong-pass   | Epic sadface: Username and password do not match any user in this service |
      |                 | secret_sauce | Epic sadface: Username is required                                        |
      | standard_user   |              | Epic sadface: Password is required                                        |
