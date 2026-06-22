package com.eatclub.payments;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatclub.driver.DriverManager;

/**
 * Drives Apple Pay on a BrowserStack session via the {@code browserstack_executor}
 * JS commands. The session must be created with {@code enableApplePay: true} (see
 * {@code DriverFactory}, gated by {@code -Dbstack.applePay=true}); BrowserStack then
 * pre-loads a sandbox test card + passcode and handles the AssistiveTouch / side
 * button / passcode interactions automatically.
 *
 * Docs: https://www.browserstack.com/docs/app-automate/appium/apple-pay
 *
 * Usage (after the Apple Pay sheet is shown by tapping the in-app Apple Pay button):
 * <pre>
 *   ApplePay applePay = new ApplePay();
 *   applePay.setDetails(ApplePayDetails.sample());
 *   applePay.confirmPayment();
 * </pre>
 */
public final class ApplePay {

    private static final Logger log = LoggerFactory.getLogger(ApplePay.class);
    private static final Json JSON = new Json();

    private final JavascriptExecutor js;

    public ApplePay() {
        this.js = (JavascriptExecutor) DriverManager.getDriver();
    }

    /** Fill the Apple Pay sheet's shipping / billing / contact details. */
    public void setDetails(ApplePayDetails details) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("shippingDetails", details.shipping());
        arguments.put("billingDetails", details.billing());
        arguments.put("contact", details.contact());
        log.info("Setting Apple Pay details");
        execute("applePayDetails", arguments);
    }

    /** Confirm/authorise the payment (triggers the side-button + passcode flow). */
    public void confirmPayment() {
        log.info("Confirming Apple Pay payment");
        execute("applePay", Map.of("confirmPayment", "true"));
    }

    private void execute(String action, Map<String, Object> arguments) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("arguments", arguments);
        js.executeScript("browserstack_executor: " + JSON.toJson(payload));
    }
}
