@wikipedia @smoke
Feature: Wikipedia search
  As a Wikipedia user
  I want to search for articles
  So that I can find information quickly

  Background:
    Given the Wikipedia app is launched

  @search
  Scenario: Search returns results for a valid term
    When I open the search bar
    And I search for "BrowserStack"
    Then I should see search results

  @search @data
  Scenario Outline: Search returns results for multiple terms
    When I open the search bar
    And I search for "<term>"
    Then I should see at least 1 search results

    Examples:
      | term      |
      | Appium    |
      | Selenium  |
      | Bengaluru |
