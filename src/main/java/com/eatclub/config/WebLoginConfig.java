package com.eatclub.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openqa.selenium.By;

/**
 * Configuration for the reusable web SMS-OTP login sample
 * ({@code WebOtpLoginPage} / {@code WebOtpLoginSteps}).
 *
 * A customer adapts this sample to their own login page WITHOUT touching code:
 * edit {@code src/test/resources/config/web-otp-login.properties} (the URL, the
 * element locators, the OTP length, the SMS sender), or override any single key
 * on the command line, e.g. {@code -Dweblogin.url=https://your.site/login}.
 *
 * Precedence per key: system property ({@code -Dweblogin.*}) → properties file.
 *
 * Locator values accept a type prefix: {@code css=...}, {@code xpath=...},
 * {@code id=...} or {@code name=...}. With no prefix, a value starting with
 * {@code //} or {@code (} is treated as XPath, otherwise as a CSS selector.
 */
public final class WebLoginConfig {

    private static final String RESOURCE = "config/web-otp-login.properties";

    private final Properties props = new Properties();

    public WebLoginConfig() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + RESOURCE, e);
        }
    }

    public String url() {
        return require("weblogin.url");
    }

    public int otpLength() {
        return Integer.parseInt(getOrDefault("weblogin.otpLength", "6"));
    }

    /** National-number length (digits to enter, country code stripped). */
    public int phoneDigits() {
        return Integer.parseInt(getOrDefault("weblogin.phoneDigits", "10"));
    }

    /** Substring identifying the OTP SMS sender/body; empty matches any SMS. */
    public String smsSender() {
        return get("weblogin.smsSender");
    }

    /** Substring of an abuse/rate-limit/error message that means the request was rejected. */
    public String blockedText() {
        return getOrDefault("weblogin.blockedText", "blocked");
    }

    public By loginButton()      { return toBy(require("weblogin.loginButton")); }
    public By mobileInput()      { return toBy(require("weblogin.mobileInput")); }
    public By requestOtpButton() { return toBy(require("weblogin.requestOtpButton")); }
    public By otpBox()           { return toBy(require("weblogin.otpBox")); }
    public By verifyButton()     { return toBy(require("weblogin.verifyButton")); }

    // ------------------------------------------------------------------ helpers

    /** System property override → properties file → "". */
    public String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isEmpty()) {
            return sys;
        }
        return props.getProperty(key, "");
    }

    private String getOrDefault(String key, String defaultValue) {
        String v = get(key);
        return v.isEmpty() ? defaultValue : v;
    }

    private String require(String key) {
        String v = get(key);
        if (v.isEmpty()) {
            throw new IllegalStateException("Missing config '" + key + "'. Set it in " + RESOURCE
                    + " or pass -D" + key + "=...");
        }
        return v;
    }

    /** Parse a locator spec into a Selenium {@link By}. */
    static By toBy(String spec) {
        String s = spec.trim();
        if (s.startsWith("xpath=")) return By.xpath(s.substring(6));
        if (s.startsWith("css="))   return By.cssSelector(s.substring(4));
        if (s.startsWith("id="))    return By.id(s.substring(3));
        if (s.startsWith("name="))  return By.name(s.substring(5));
        if (s.startsWith("//") || s.startsWith("(")) return By.xpath(s);
        return By.cssSelector(s);
    }
}
