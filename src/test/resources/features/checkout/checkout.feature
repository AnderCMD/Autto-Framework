@checkout @regression
Feature: Checkout
  As a customer with products in the cart
  I want to complete the checkout
  So that my order is placed

  Background:
    Given the customer is logged in as "standard"

  @smoke @critical
  Scenario: Complete a purchase with random customer data
    Given the customer has added "Sauce Labs Backpack" to the cart
    And the customer has added "Sauce Labs Onesie" to the cart
    When the customer checks out with random customer information
    Then the order summary total equals the sum of the item prices plus tax
    When the customer finishes the order
    Then the confirmation message "Thank you for your order!" is displayed

  Scenario: Checkout requires the customer information
    Given the customer has added "Sauce Labs Backpack" to the cart
    When the customer checks out without filling the form
    Then the checkout error "Error: First Name is required" is shown
