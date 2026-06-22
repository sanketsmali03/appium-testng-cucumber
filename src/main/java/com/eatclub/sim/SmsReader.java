package com.eatclub.sim;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatclub.driver.DriverManager;
import com.eatclub.enums.Platform;

import io.appium.java_client.AppiumDriver;

/**
 * Reads the OTP that arrives as a REAL SMS on a BrowserStack SIM device.
 *
 * There is no API for the SMS body. The primary, most robust strategy (Android) is
 * to read the inbox directly via {@code mobile: listSms} (see {@link #waitForOtp}).
 * As a fallback, {@link #readFromNotifications} pulls down the notification shade
 * and regexes the OTP out of the page source.
 *
 * iOS note: iOS does not expose notifications/SMS content to Appium reliably. The
 * usual iOS pattern is to let the keyboard's QuickType bar auto-fill the OTP and
 * read the OTP field value directly, or open the Messages app. The notification
 * helper therefore guards against iOS.
 */
public final class SmsReader {

    private static final Logger log = LoggerFactory.getLogger(SmsReader.class);

    private final AppiumDriver driver;
    private final Platform platform;

    public SmsReader() {
        this.driver = DriverManager.getDriver();
        this.platform = Platform.current();
    }

    /**
     * Wait for an SMS to arrive and return the OTP found in the Android
     * notification shade.
     *
     * @param digits  expected OTP length (e.g. 4 or 6)
     * @param timeout how long to wait for the SMS to land
     * @return the extracted OTP
     */
    public String readFromNotifications(int digits, Duration timeout) {
        if (!platform.isAndroid()) {
            throw new UnsupportedOperationException(
                    "Notification-based SMS reading is Android-only. On iOS, let QuickType auto-fill the OTP "
                            + "and read the field value instead, or open the Messages app.");
        }

        log.info("Waiting up to {}s for an SMS containing a {}-digit code...", timeout.getSeconds(), digits);
        Pattern otpPattern = Pattern.compile("(?<!\\d)(\\d{" + digits + "})(?!\\d)");

        FluentWait<AppiumDriver> wait = new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(Duration.ofSeconds(2));

        String otp = wait.until(d -> {
            openNotifications();
            Matcher matcher = otpPattern.matcher(d.getPageSource());
            return matcher.find() ? matcher.group(1) : null;
        });

        log.info("Read OTP from SMS notification: {}", mask(otp));
        closeNotifications();
        return otp;
    }

    /**
     * Snapshot the date (epoch millis, device clock) of the most recent matching
     * SMS already on the device. Call this BEFORE requesting a fresh OTP so
     * {@link #waitForOtp} can ignore stale codes from earlier runs.
     *
     * @param senderContains case-insensitive substring to match the SMS sender/body
     *                       (e.g. an "OTP" sender id); pass "" to match any SMS.
     */
    public long latestSmsDate(String senderContains) {
        long newest = 0L;
        for (Map<String, Object> sms : listSms(10)) {
            if (matches(sms, senderContains)) {
                newest = Math.max(newest, dateOf(sms));
            }
        }
        return newest;
    }

    /**
     * Poll the device inbox via {@code mobile: listSms} until an SMS newer than
     * {@code afterDate} arrives from the expected sender, and return its OTP.
     *
     * This reads the SMS body directly (no notification-shade scraping), which is
     * far more robust. Works on the BrowserStack SIM device.
     *
     * @param digits         OTP length (e.g. 4 or 6)
     * @param senderContains substring to identify the OTP sender (e.g. "frchrg")
     * @param afterDate      ignore messages at/older than this (from {@link #latestSmsDate})
     * @param timeout        how long to wait for the SMS to land
     */
    public String waitForOtp(int digits, String senderContains, long afterDate, Duration timeout) {
        log.info("Waiting up to {}s for a {}-digit OTP SMS from '{}'...",
                timeout.getSeconds(), digits, senderContains);
        FluentWait<AppiumDriver> wait = new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(Duration.ofSeconds(2))
                .ignoring(RuntimeException.class);

        String otp = wait.until(d -> {
            Map<String, Object> freshest = null;
            for (Map<String, Object> sms : listSms(10)) {
                if (matches(sms, senderContains) && dateOf(sms) > afterDate) {
                    if (freshest == null || dateOf(sms) > dateOf(freshest)) {
                        freshest = sms;
                    }
                }
            }
            if (freshest == null) {
                return null;
            }
            String body = String.valueOf(freshest.get("body"));
            Matcher m = Pattern.compile("(?<!\\d)(\\d{" + digits + "})(?!\\d)").matcher(body);
            return m.find() ? m.group(1) : null;
        });
        log.info("Read OTP from SMS: {}", mask(otp));
        return otp;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listSms(int max) {
        Object res = ((JavascriptExecutor) driver).executeScript("mobile: listSms", Map.of("max", max));
        if (res instanceof Map && ((Map<String, Object>) res).get("items") instanceof List) {
            return (List<Map<String, Object>>) ((Map<String, Object>) res).get("items");
        }
        return List.of();
    }

    private static boolean matches(Map<String, Object> sms, String senderContains) {
        if (senderContains == null || senderContains.isEmpty()) {
            return true;
        }
        String needle = senderContains.toLowerCase();
        String address = String.valueOf(sms.getOrDefault("address", "")).toLowerCase();
        String body = String.valueOf(sms.getOrDefault("body", "")).toLowerCase();
        return address.contains(needle) || body.contains(needle);
    }

    private static long dateOf(Map<String, Object> sms) {
        Object date = sms.get("date");
        try {
            return date instanceof Number ? ((Number) date).longValue() : Long.parseLong(String.valueOf(date));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** Extract an N-digit OTP from an arbitrary message string. */
    public static String extractOtp(String message, int digits) {
        Matcher matcher = Pattern.compile("(?<!\\d)(\\d{" + digits + "})(?!\\d)").matcher(message);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "No " + digits + "-digit OTP found in message: " + message);
        }
        return matcher.group(1);
    }

    private void openNotifications() {
        ((JavascriptExecutor) driver).executeScript("mobile: openNotifications");
    }

    private void closeNotifications() {
        // Dismiss the shade so it doesn't sit over the app under test.
        ((JavascriptExecutor) driver).executeScript("mobile: pressKey", java.util.Map.of("keycode", 4));
    }

    private static String mask(String otp) {
        return otp == null || otp.length() < 2 ? "****" : otp.charAt(0) + "***" + otp.charAt(otp.length() - 1);
    }
}
