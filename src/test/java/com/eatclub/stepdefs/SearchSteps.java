package com.eatclub.stepdefs;

import org.testng.Assert;

import com.eatclub.pages.SearchPage;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for Wikipedia search. Platform-agnostic: the same steps run
 * on Android and iOS because SearchPage carries dual locators.
 *
 * Page objects are constructed lazily here (driver already set by the @Before
 * hook). For larger suites, inject shared state via PicoContainer instead.
 */
public class SearchSteps {

    private SearchPage searchPage;
    private int lastResultCount;

    @Given("the Wikipedia app is launched")
    public void theWikipediaAppIsLaunched() {
        // App is started by the driver session; instantiate the page object.
        searchPage = new SearchPage();
    }

    @When("I open the search bar")
    public void iOpenTheSearchBar() {
        searchPage.openSearch();
    }

    @When("I search for {string}")
    public void iSearchFor(String query) {
        searchPage.searchFor(query);
    }

    @Then("I should see search results")
    public void iShouldSeeSearchResults() {
        Assert.assertTrue(searchPage.hasResults(),
                "Expected at least one search result but found none.");
    }

    @Then("I should see at least {int} search results")
    public void iShouldSeeAtLeastResults(int expected) {
        lastResultCount = searchPage.resultCount();
        Assert.assertTrue(lastResultCount >= expected,
                "Expected at least " + expected + " results but found " + lastResultCount);
    }
}
