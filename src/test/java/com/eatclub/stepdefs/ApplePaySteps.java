package com.eatclub.stepdefs;

import org.testng.Assert;

import com.eatclub.pages.ApplePayCheckoutPage;
import com.eatclub.payments.ApplePay;
import com.eatclub.payments.ApplePayDetails;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Sample/template steps for an Apple Pay checkout on iOS.
 *
 * Flow: open checkout → tap the in-app Apple Pay button (presents the system
 * sheet) → fill the sheet's details via the BrowserStack executor → confirm the
 * payment → assert the app shows an order confirmation.
 *
 * Requires an Apple Pay-entitled iOS app and an Apple Pay-enabled session. Run:
 *   mvn test -Pios -Dbstack.applePay=true -Dbstack.app=bs://<your-app> \
 *       -Dcucumber.filter.tags="@applepay"
 *
 * The "Given the mobile app is launched" step is reused from {@code WebOtpLoginSteps}.
 */
public class ApplePaySteps {

    private ApplePayCheckoutPage checkout;

    @When("I start checkout and tap the Apple Pay button")
    public void iStartCheckoutAndTapTheApplePayButton() {
        checkout = new ApplePayCheckoutPage();
        checkout.openCheckout().tapApplePayButton();
    }

    @When("I provide Apple Pay shipping, billing and contact details")
    public void iProvideApplePayDetails() {
        new ApplePay().setDetails(ApplePayDetails.sample());
    }

    @When("I confirm the Apple Pay payment")
    public void iConfirmTheApplePayPayment() {
        new ApplePay().confirmPayment();
    }

    @Then("the payment should be successful")
    public void thePaymentShouldBeSuccessful() {
        Assert.assertTrue(checkout.isPaymentSuccessful(),
                "Expected an order confirmation after the Apple Pay payment");
    }
}
