package com.eatclub.pages;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.eatclub.driver.DriverManager;
import com.eatclub.enums.Platform;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

/**
 * Base for all page objects.
 *
 * - Initialises Appium PageFactory so {@code @AndroidFindBy}/{@code @iOSXCUITFindBy}
 *   fields resolve to the correct locator for the active platform.
 * - Centralises explicit waits. Never use Thread.sleep in page objects.
 */
public abstract class BasePage {

    protected final AppiumDriver driver;
    protected final WebDriverWait wait;
    protected final Platform platform;

    protected BasePage() {
        this.driver = DriverManager.getDriver();
        this.platform = Platform.current();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        // 10s implicit element-lookup window used by PageFactory proxies.
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ofSeconds(10)), this);
    }

    protected WebElement waitForVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected void tap(WebElement element) {
        waitForClickable(element).click();
    }

    protected void type(WebElement element, String text) {
        waitForVisible(element).sendKeys(text);
    }

    protected boolean isDisplayed(WebElement element) {
        try {
            return waitForVisible(element).isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    protected int countOf(List<WebElement> elements) {
        // Wait until at least one element is present, then return the size.
        wait.until(d -> !elements.isEmpty());
        return elements.size();
    }
}
