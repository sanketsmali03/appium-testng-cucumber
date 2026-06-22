package com.eatclub.stepdefs;

import java.time.Duration;

import org.testng.Assert;

import com.eatclub.config.WebLoginConfig;
import com.eatclub.pages.WebOtpLoginPage;
import com.eatclub.sim.BrowserStackSim;
import com.eatclub.sim.SmsReader;
import com.eatclub.web.ChromeSession;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Reusable steps for a web SMS-OTP login, run from inside the app session by
 * switching to Chrome (see {@link ChromeSession}).
 *
 * Flow: launch the app → open the configured login page in Chrome → read the
 * device's real SIM number → enter it and request an OTP → read the OTP from the
 * SIM's SMS inbox ({@code mobile: listSms}) → submit it → assert it was accepted.
 *
 * Configure the site + locators in {@code config/web-otp-login.properties}.
 * Requires a SIM-enabled build. Run with:
 *   mvn test -Pandroid -Dbstack.sim=true -Dcucumber.filter.tags="@otp"
 */
public class WebOtpLoginSteps {

    private static final Duration SMS_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(30);

    private final WebLoginConfig cfg = new WebLoginConfig();
    private final ChromeSession chrome = new ChromeSession();
    private WebOtpLoginPage loginPage;
    private String phoneNumber;

    @Given("the mobile app is launched")
    public void theMobileAppIsLaunched() {
        // The app session is started by the @Before hook; nothing to do here.
    }

    @When("I open the login page in Chrome")
    public void iOpenTheLoginPageInChrome() {
        // Read the SIM number first, while still in the native app context (avoids a
        // native<->web round trip mid-login, which can lose the active Chrome tab).
        phoneNumber = new BrowserStackSim().nationalNumber(cfg.phoneDigits());
        chrome.openUrl(cfg.url());
        loginPage = new WebOtpLoginPage();
    }

    @When("I request an OTP for the device's SIM number")
    public void iRequestAnOtpForTheSimNumber() {
        loginPage.openLoginForm().enterMobileAndRequestOtp(phoneNumber);
        Assert.assertTrue(loginPage.isOtpScreenShown(),
                "Expected the OTP entry screen after requesting an OTP for " + phoneNumber);
    }

    @When("I read the SMS OTP and submit it")
    public void iReadTheSmsOtpAndSubmitIt() {
        // Snapshot the inbox, then read the freshly delivered OTP from the SIM.
        chrome.switchToNative();
        SmsReader sms = new SmsReader();
        long since = sms.latestSmsDate(cfg.smsSender());
        String otp = sms.waitForOtp(cfg.otpLength(), cfg.smsSender(), since, SMS_TIMEOUT);
        chrome.switchToWeb();

        loginPage.enterOtp(otp).submitOtp();
    }

    @Then("the OTP should be accepted")
    public void theOtpShouldBeAccepted() {
        Assert.assertFalse(loginPage.hasOtpError(),
                "The site reported an invalid/blocked OTP for " + phoneNumber);
        Assert.assertTrue(loginPage.passedOtpVerification(VERIFY_TIMEOUT),
                "Expected to move past the OTP entry screen after submitting the SMS OTP");
    }
}
