package com.eatclub.pages;

import java.util.List;

import org.openqa.selenium.WebElement;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

/**
 * Wikipedia search screen — a single page object that works on BOTH platforms.
 *
 * The cross-platform pattern: declare one field, annotate it with both
 * {@code @AndroidFindBy} and {@code @iOSXCUITFindBy}. Appium PageFactory picks
 * the locator matching the running platform. Step definitions stay
 * platform-agnostic.
 */
public class SearchPage extends BasePage {

    @AndroidFindBy(accessibility = "Search Wikipedia")
    @iOSXCUITFindBy(accessibility = "Search Wikipedia")
    private WebElement searchEntryPoint;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/search_src_text")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSearchField")
    private WebElement searchInput;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/page_list_item_title")
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND visible == 1")
    private List<WebElement> searchResults;

    public SearchPage openSearch() {
        tap(searchEntryPoint);
        return this;
    }

    public SearchPage searchFor(String query) {
        type(searchInput, query);
        return this;
    }

    public int resultCount() {
        return countOf(searchResults);
    }

    public boolean hasResults() {
        return resultCount() > 0;
    }
}
