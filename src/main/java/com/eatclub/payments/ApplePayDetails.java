package com.eatclub.payments;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shipping / billing / contact details handed to the Apple Pay sheet via the
 * BrowserStack {@code applePayDetails} executor.
 *
 * The maps are sent as-is, so use exactly the keys BrowserStack expects. Notes
 * from the docs: use only ONE postal-code key ({@code zip} / {@code postCode} /
 * {@code postalCode}); drop fields that don't apply to your locale; the phone
 * number must include a {@code +} country-code prefix.
 */
public final class ApplePayDetails {

    private final Map<String, Object> shipping;
    private final Map<String, Object> billing;
    private final Map<String, Object> contact;

    public ApplePayDetails(Map<String, Object> shipping,
                           Map<String, Object> billing,
                           Map<String, Object> contact) {
        this.shipping = shipping;
        this.billing = billing;
        this.contact = contact;
    }

    public Map<String, Object> shipping() { return shipping; }
    public Map<String, Object> billing()  { return billing; }
    public Map<String, Object> contact()  { return contact; }

    /**
     * Ready-to-use US sample (BrowserStack's documented example). Edit these
     * values, or build your own {@link ApplePayDetails}, for your test data.
     */
    public static ApplePayDetails sample() {
        Map<String, Object> address = address(
                "John", "Doe", "160 Amphitheatre", "Mountain View", "California", "94043", "United States");
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("email", "random@gmail.com");
        contact.put("phone", "+1-212-456-7890");
        return new ApplePayDetails(address, new LinkedHashMap<>(address), contact);
    }

    private static Map<String, Object> address(String firstName, String lastName, String street,
                                               String city, String state, String zip, String country) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("firstName", firstName);
        a.put("lastName", lastName);
        a.put("street", street);
        a.put("city", city);
        a.put("state", state);
        a.put("zip", zip);
        a.put("country", country);
        return a;
    }
}
