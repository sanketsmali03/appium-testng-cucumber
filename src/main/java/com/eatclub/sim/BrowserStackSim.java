package com.eatclub.sim;

import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatclub.driver.DriverManager;

/**
 * Reads the SIM details of the live BrowserStack session.
 *
 * BrowserStack's SIM-enabled devices carry a REAL, working SIM. When the build
 * sets {@code enableSim: true} + {@code simOptions} (see browserstack-*.yml), the
 * device is allocated a phone number that can send/receive real SMS. There is NO
 * BrowserStack API that returns the SMS body — the OTP physically arrives on the
 * device, so it must be read off the device UI (see {@link SmsReader}).
 *
 * This helper only fetches the allocated phone number / region via the
 * {@code deviceInfo} JS executor:
 * https://www.browserstack.com/docs/app-automate/appium/sim-devices
 *
 * The executor returns:
 * <pre>{"simOptions": {"Phone Number":"+1XXXXXXXXXX", "Region":"USA", "esim":true}}</pre>
 */
public final class BrowserStackSim {

    private static final Logger log = LoggerFactory.getLogger(BrowserStackSim.class);

    /** JS executor payload that asks BrowserStack for the device's SIM details. */
    private static final String DEVICE_INFO_SCRIPT =
            "browserstack_executor: {\"action\":\"deviceInfo\","
                    + "\"arguments\":{\"deviceProperties\":[\"simOptions\"]}}";

    private final JavascriptExecutor js;

    public BrowserStackSim() {
        this.js = (JavascriptExecutor) DriverManager.getDriver();
    }

    /**
     * @return the phone number allocated to the SIM device, exactly as BrowserStack
     *         returns it (e.g. {@code +1XXXXXXXXXX}).
     * @throws IllegalStateException if the session has no SIM (build missing
     *         {@code enableSim}) or runs outside BrowserStack.
     */
    public String phoneNumber() {
        Map<String, Object> simOptions = simOptions();
        Object number = simOptions.get("Phone Number");
        if (number == null || number.toString().isBlank()) {
            throw new IllegalStateException(
                    "BrowserStack returned no SIM phone number. Is enableSim set in browserstack-*.yml "
                            + "and is the device SIM-enabled? Raw simOptions=" + simOptions);
        }
        String value = number.toString().trim();
        log.info("Allocated SIM phone number: {}", value);
        return value;
    }

    /** @return the phone number stripped to digits only (drops '+', spaces, dashes). */
    public String phoneNumberDigits() {
        return phoneNumber().replaceAll("[^0-9]", "");
    }

    /**
     * @return the national (subscriber) number: the last {@code length} digits,
     *         i.e. the number without its country code (many web login forms
     *         expect the national number only, e.g. the 10-digit number without +91).
     */
    public String nationalNumber(int length) {
        String digits = phoneNumberDigits();
        return digits.length() <= length ? digits : digits.substring(digits.length() - length);
    }

    /** @return the full {@code simOptions} map (Phone Number, Region, esim, ...). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> simOptions() {
        Object raw = js.executeScript(DEVICE_INFO_SCRIPT);
        // Selenium deserialises a JSON object result into a Map. BrowserStack may return
        // the SIM map directly ({"Phone Number":..,"Region":..}) or wrapped under a
        // "simOptions" key. Handle both shapes.
        if (raw instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) raw;
            Object wrapped = map.get("simOptions");
            if (wrapped instanceof Map) {
                return (Map<String, Object>) wrapped;
            }
            if (map.containsKey("Phone Number")) {
                return map;
            }
        }
        throw new IllegalStateException(
                "Could not read simOptions from the BrowserStack session. "
                        + "Ensure the build enables SIM and runs on a SIM-capable device. Raw response=" + raw);
    }
}
