@inventory @regression
Feature: Product catalog
  As a customer
  I want to sort and pick products from the catalog
  So that I can quickly find what I want to buy

  Background:
    Given the customer is logged in as "standard"

  @smoke
  Scenario: Sort products by price from low to high
    When the customer sorts the products by "Price (low to high)"
    Then the products are sorted by price in ascending order

  Scenario: Sort products by name from Z to A
    When the customer sorts the products by "Name (Z to A)"
    Then the products are sorted by name in descending order

  Scenario: Add several products to the cart
    When the customer adds the following products to the cart:
      | Sauce Labs Backpack     |
      | Sauce Labs Bike Light   |
      | Sauce Labs Bolt T-Shirt |
    Then the cart badge shows 3 products

  @demo-failure @known-bug
  Scenario: Problem user sees the right product images (fails on purpose to showcase the report)
    Given the customer is logged in as "problem"
    Then every product shows its own image
