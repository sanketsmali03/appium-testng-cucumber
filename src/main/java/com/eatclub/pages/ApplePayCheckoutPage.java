package com.eatclub.pages;

import org.openqa.selenium.WebElement;

import io.appium.java_client.pagefactory.iOSXCUITFindBy;

/**
 * Checkout screen of an Apple Pay-enabled iOS app.
 *
 * <p><b>Locators below are PLACEHOLDERS.</b> Replace the {@code accessibility}
 * values with your app's real ones (inspect with Appium Inspector / BrowserStack
 * App Live). Apple Pay is iOS-only, so only {@code @iOSXCUITFindBy} is used.
 *
 * <p>The page drives the in-app part of the flow: open checkout and tap the
 * "Pay with Apple Pay" button (which presents the system Apple Pay sheet). The
 * sheet itself is then driven via {@link com.eatclub.payments.ApplePay}.
 */
public class ApplePayCheckoutPage extends BasePage {

    @iOSXCUITFindBy(accessibility = "checkout_button")
    private WebElement checkoutButton;

    @iOSXCUITFindBy(accessibility = "apple_pay_button")
    private WebElement applePayButton;

    // Element shown only after a successful payment (e.g. an order/receipt screen).
    @iOSXCUITFindBy(accessibility = "order_confirmation")
    private WebElement orderConfirmation;

    public ApplePayCheckoutPage openCheckout() {
        tap(checkoutButton);
        return this;
    }

    /** Tap the in-app Apple Pay button to present the system payment sheet. */
    public ApplePayCheckoutPage tapApplePayButton() {
        tap(applePayButton);
        return this;
    }

    public boolean isPaymentSuccessful() {
        return isDisplayed(orderConfirmation);
    }
}
