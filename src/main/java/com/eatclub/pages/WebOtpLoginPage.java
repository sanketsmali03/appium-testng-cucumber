package com.eatclub.pages;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatclub.config.WebLoginConfig;
import com.eatclub.driver.DriverManager;

import io.appium.java_client.AppiumDriver;

/**
 * Reusable page object for a web SMS-OTP login, driven inside Chrome's webview
 * (plain Selenium {@code By} locators - NOT Appium native locators).
 *
 * Generic flow: open the login form → enter the phone number → request the OTP
 * → enter the OTP into one or more boxes → verify. All locators, the URL, the OTP
 * length and the SMS sender come from {@link WebLoginConfig} so a customer adapts
 * this to their own site purely through configuration. The interaction logic
 * tolerates the loading spinners and auto-advancing forms common to such pages.
 */
public class WebOtpLoginPage {

    private static final Logger log = LoggerFactory.getLogger(WebOtpLoginPage.class);

    private final AppiumDriver driver;
    private final WebDriverWait wait;
    private final WebLoginConfig cfg;

    public WebOtpLoginPage() {
        this.driver = (AppiumDriver) DriverManager.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        this.cfg = new WebLoginConfig();
    }

    /** Open the login form and wait for the mobile-number field. */
    public WebOtpLoginPage openLoginForm() {
        clickRobustly(cfg.loginButton());
        wait.until(ExpectedConditions.visibilityOfElementLocated(cfg.mobileInput()));
        return this;
    }

    /** Enter the phone number and request the OTP; waits for the OTP field(s). */
    public WebOtpLoginPage enterMobileAndRequestOtp(String phoneNumber) {
        WebElement mobile = wait.until(ExpectedConditions.visibilityOfElementLocated(cfg.mobileInput()));
        mobile.clear();
        mobile.sendKeys(phoneNumber);
        log.info("Entered phone number {}, requesting OTP", phoneNumber);

        // Some forms auto-send the OTP once the number is complete (a spinner shows,
        // then the OTP screen). Give that a short chance before clicking the button.
        if (!appearsWithin(cfg.otpBox(), Duration.ofSeconds(6))) {
            clickRobustly(cfg.requestOtpButton());
        }
        waitForOtpScreen();
        return this;
    }

    public void waitForOtpScreen() {
        // Poll quickly so a transient block/error toast is caught (often visible only ~5s).
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .pollingEvery(Duration.ofMillis(250))
                .until(d -> {
                    if (!d.findElements(cfg.otpBox()).isEmpty()) {
                        return true;
                    }
                    WebElement blocked = firstBlockingMessage();
                    if (blocked != null) {
                        throw new IllegalStateException(
                                "The site rejected the OTP request: \"" + blocked.getText().trim()
                              + "\". This usually means anti-abuse/rate limiting on the phone number or IP "
                              + "(configurable via weblogin.blockedText). Retry later or with a fresh number.");
                    }
                    return null;
                });
    }

    public boolean isOtpScreenShown() {
        return !driver.findElements(cfg.otpBox()).isEmpty();
    }

    /** Type the OTP - one digit per box when there are several, else into a single field. */
    public WebOtpLoginPage enterOtp(String otp) {
        waitForOtpScreen();
        List<WebElement> boxes = driver.findElements(cfg.otpBox());
        if (boxes.size() <= 1) {
            WebElement field = boxes.isEmpty()
                    ? wait.until(ExpectedConditions.visibilityOfElementLocated(cfg.otpBox()))
                    : boxes.get(0);
            field.click();
            field.sendKeys(otp);
        } else {
            for (int i = 0; i < boxes.size() && i < otp.length(); i++) {
                WebElement box = boxes.get(i);
                box.click();
                box.sendKeys(String.valueOf(otp.charAt(i)));
            }
        }
        log.info("Entered {}-digit OTP into {} field(s)", otp.length(), boxes.size());
        return this;
    }

    /** Submit the OTP (some layouts auto-submit on the final digit). */
    public void submitOtp() {
        List<WebElement> verify = driver.findElements(cfg.verifyButton());
        if (!verify.isEmpty() && verify.get(0).isEnabled()) {
            clickRobustly(cfg.verifyButton());
            log.info("Clicked verify");
        } else {
            log.info("Verify button absent/disabled - assuming auto-submit on final digit");
        }
    }

    public boolean hasOtpError() {
        return firstBlockingMessage() != null;
    }

    /**
     * Success criterion: the OTP-entry screen is left behind (boxes gone) and no
     * block/error message is shown.
     */
    public boolean passedOtpVerification(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d -> d.findElements(cfg.otpBox()).isEmpty());
            return !hasOtpError();
        } catch (TimeoutException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    private WebElement firstBlockingMessage() {
        String needle = cfg.blockedText();
        if (needle == null || needle.isEmpty()) {
            return null;
        }
        String lower = needle.toLowerCase();
        // translate() makes the XPath case-insensitive for ASCII letters.
        By blocking = By.xpath("//*[contains(translate(., "
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), '" + lower + "')]");
        List<WebElement> hits = driver.findElements(blocking);
        return hits.isEmpty() ? null : hits.get(0);
    }

    /**
     * Click an element reliably. Such pages briefly overlay a loading spinner that
     * intercepts clicks; a JS click would bypass it but may not trigger the page's
     * handler, so we retry a real click until the spinner clears and it lands.
     */
    private void clickRobustly(By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(40))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(d -> {
                    try {
                        WebElement el = d.findElement(locator);
                        if (!el.isDisplayed() || !el.isEnabled()) {
                            return false;
                        }
                        el.click();
                        return true;
                    } catch (org.openqa.selenium.ElementClickInterceptedException
                             | org.openqa.selenium.NoSuchElementException retry) {
                        return false;
                    }
                });
    }

    private boolean appearsWithin(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.numberOfElementsToBeMoreThan(locator, 0));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
