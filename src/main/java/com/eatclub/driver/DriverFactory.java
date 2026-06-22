package com.eatclub.driver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.eatclub.config.ConfigManager;
import com.eatclub.enums.Platform;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

/**
 * Creates the right {@link AppiumDriver} for the current platform and execution
 * target.
 *
 * BrowserStack path: connects directly to the BrowserStack App Automate hub with
 * explicit W3C capabilities (device, OS, app, bstack:options). Credentials come
 * from the BROWSERSTACK_USERNAME / BROWSERSTACK_ACCESS_KEY env vars; device, OS,
 * and app are overridable via -D system properties (see {@link #bs(String, String)}).
 *
 * (Historically this project relied on the BrowserStack Java SDK -javaagent to
 * auto-instrument driver creation from browserstack.yml. That auto-instrumentation
 * relaunches the JVM via a native CLI, which is incompatible with the Surefire
 * forked test JVM in some local environments — the fork exits before any session
 * is created. The direct connection below is environment-independent.)
 *
 * Local path: full capabilities are read from config/local-*.properties and the
 * driver connects to a local Appium server.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static AppiumDriver create() {
        Platform platform = Platform.current();
        return ConfigManager.isBrowserStack()
                ? createForBrowserStack(platform)
                : createForLocal(platform);
    }

    // ------------------------------------------------------------------ BrowserStack

    private static AppiumDriver createForBrowserStack(Platform platform) {
        String user = requireEnv("BROWSERSTACK_USERNAME");
        String key = requireEnv("BROWSERSTACK_ACCESS_KEY");
        URL hub = url("https://hub-cloud.browserstack.com/wd/hub");

        Map<String, Object> bstack = new HashMap<>();
        bstack.put("userName", user);
        bstack.put("accessKey", key);
        bstack.put("projectName", bs("bstack.project", "EatClub Appium TestNG Cucumber"));
        bstack.put("buildName", bs("bstack.build", "eatclub-" + platform.name().toLowerCase()));
        bstack.put("sessionName", bs("bstack.session", "search.feature"));
        bstack.put("debug", true);
        bstack.put("networkLogs", true);

        // SIM allocation for real SMS/OTP testing. Enable with -Dbstack.sim=true.
        // Requires a SIM-capable device (default Galaxy S23 qualifies) and a
        // Device Cloud Pro plan. Region defaults to India; override with -Dbstack.sim.region.
        // Docs: https://www.browserstack.com/docs/app-automate/appium/sim-devices
        if (Boolean.parseBoolean(bs("bstack.sim", "false"))) {
            bstack.put("enableSim", true);
            Map<String, Object> simOptions = new HashMap<>();
            simOptions.put("region", bs("bstack.sim.region", "India"));
            bstack.put("simOptions", simOptions);
        }

        if (platform.isAndroid()) {
            UiAutomator2Options options = new UiAutomator2Options();
            options.setPlatformName("android");
            options.setDeviceName(bs("bstack.device", "Samsung Galaxy S23"));
            options.setPlatformVersion(bs("bstack.osVersion", "13.0"));
            options.setApp(bs("bstack.app", "bs://3378f73357627bf77dcbe446e4b48bbea0515bf4"));
            options.setCapability("bstack:options", bstack);
            return new AndroidDriver(hub, options);
        }

        XCUITestOptions options = new XCUITestOptions();
        options.setPlatformName("ios");
        options.setDeviceName(bs("bstack.device", "iPhone 15"));
        options.setPlatformVersion(bs("bstack.osVersion", "17"));
        options.setApp(bs("bstack.app", "bs://3378f73357627bf77dcbe446e4b48bbea0515bf4"));

        // Apple Pay testing (private beta; Device Cloud Pro+). Enable with
        // -Dbstack.applePay=true. The app must be Enterprise-signed with the Apple Pay
        // entitlement, so resignApp is left false. BrowserStack pre-loads a sandbox
        // test card + passcode. Docs: https://www.browserstack.com/docs/app-automate/appium/apple-pay
        if (Boolean.parseBoolean(bs("bstack.applePay", "false"))) {
            bstack.put("enableApplePay", true);
            bstack.put("resignApp", false);
            bstack.put("applePayPreferredNetworks", new String[] {"Visa", "Mastercard"});
            // Presents the payment sheet reliably after tapping the Apple Pay button.
            options.setCapability("appium:nativeWebTap", true);
        }

        options.setCapability("bstack:options", bstack);
        return new IOSDriver(hub, options);
    }

    /** System property override -> default. */
    private static String bs(String sysProp, String defaultValue) {
        String v = System.getProperty(sysProp);
        return (v == null || v.isEmpty()) ? defaultValue : v;
    }

    private static String requireEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required env var " + name + " for BrowserStack execution");
        }
        return v;
    }

    // ------------------------------------------------------------------ Local

    private static AppiumDriver createForLocal(Platform platform) {
        ConfigManager config = ConfigManager.forPlatform(platform);
        URL appiumServer = url(config.get("appium.server.url", "http://127.0.0.1:4723"));
        Duration commandTimeout = Duration.ofSeconds(
                Long.parseLong(config.get("newCommandTimeout", "120")));

        if (platform.isAndroid()) {
            UiAutomator2Options options = new UiAutomator2Options()
                    .setDeviceName(config.get("deviceName", "Android Emulator"))
                    .setApp(config.get("app"))
                    .setAppPackage(config.get("appPackage", "org.wikipedia.alpha"))
                    .setAppActivity(config.get("appActivity", "org.wikipedia.main.MainActivity"))
                    .setAutoGrantPermissions(true)
                    .setNewCommandTimeout(commandTimeout);
            return new AndroidDriver(appiumServer, options);
        }

        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(config.get("deviceName", "iPhone 15"))
                .setPlatformVersion(config.get("platformVersion", "17"))
                .setApp(config.get("app"))
                .setBundleId(config.get("bundleId", "org.wikimedia.wikipedia"))
                .setAutoAcceptAlerts(true)
                .setNewCommandTimeout(commandTimeout);
        return new IOSDriver(appiumServer, options);
    }

    private static URL url(String spec) {
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad Appium/hub URL: " + spec, e);
        }
    }
}
