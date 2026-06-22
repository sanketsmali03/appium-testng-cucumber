package com.eatclub.web;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatclub.driver.DriverManager;

import io.appium.java_client.android.AndroidDriver;

/**
 * Drives the on-device Chrome browser from within an App Automate session.
 *
 * The test session is a native app session (Wikipedia is the app under test).
 * To exercise a web flow we launch Chrome via an Android deep link, then switch
 * the Appium context to Chrome's webview so the page can be automated with
 * ordinary Selenium {@code By} locators. Switching back to {@code NATIVE_APP}
 * lets us read the device SIM / SMS.
 *
 * Notes learned on BrowserStack (UiAutomator2):
 *  - {@code mobile: activateApp} is NOT available on this driver; {@code mobile: deepLink} is.
 *  - The device's Chrome can have several tabs, so after switching to the webview
 *    we explicitly navigate the active window to the target URL (deterministic).
 */
public final class ChromeSession {

    private static final Logger log = LoggerFactory.getLogger(ChromeSession.class);

    private static final String CHROME_PACKAGE = "com.android.chrome";
    private static final String NATIVE_CONTEXT = "NATIVE_APP";
    private static final String CHROME_WEBVIEW_PREFIX = "WEBVIEW_chrome";

    private final AndroidDriver driver;
    private String activeUrl;

    public ChromeSession() {
        this.driver = (AndroidDriver) DriverManager.getDriver();
    }

    /**
     * Launch Chrome at {@code url}, switch into its webview, and make the active
     * tab show {@code url}. Leaves the driver in the Chrome webview context.
     */
    public void openUrl(String url) {
        log.info("Opening {} in Chrome", url);
        this.activeUrl = url;
        driver.executeScript("mobile: deepLink", Map.of("url", url, "package", CHROME_PACKAGE));
        switchToWebContext();
        Set<String> handles = driver.getWindowHandles();
        if (!handles.isEmpty()) {
            driver.switchTo().window(handles.iterator().next());
        }
        // Force the active tab to the target URL regardless of which tab Chrome opened.
        driver.get(url);
    }

    /**
     * Switch the Appium context to Chrome's webview, then focus the window/tab
     * showing the most recently opened URL (Chrome may hold several tabs).
     */
    public void switchToWeb() {
        switchToWebContext();
        focusActiveUrlWindow();
    }

    private void switchToWebContext() {
        String web = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(40))
                .pollingEvery(Duration.ofSeconds(1))
                .until(d -> d.getContextHandles().stream()
                        .filter(c -> c.startsWith(CHROME_WEBVIEW_PREFIX))
                        .findFirst().orElse(null));
        log.info("Switching to webview context: {}", web);
        driver.context(web);
    }

    /** Among the webview's windows, select the one whose URL matches activeUrl's host. */
    private void focusActiveUrlWindow() {
        if (activeUrl == null) {
            return;
        }
        String host = hostOf(activeUrl);
        Boolean found = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(RuntimeException.class)
                .until(d -> {
                    for (String handle : d.getWindowHandles()) {
                        try {
                            d.switchTo().window(handle);
                            Object href = d.executeScript("return location.href");
                            if (href != null && href.toString().contains(host)) {
                                return true;
                            }
                        } catch (RuntimeException ignore) {
                            // window vanished / not ready; try the next one
                        }
                    }
                    return null; // keep polling until a matching window appears
                });
        log.info("Focused target-URL window: {}", found);
    }

    private static String hostOf(String url) {
        String s = url.replaceFirst("^https?://", "");
        int slash = s.indexOf('/');
        return slash > 0 ? s.substring(0, slash) : s;
    }

    /** Switch back to the native context (required to read SIM number / SMS). */
    public void switchToNative() {
        driver.context(NATIVE_CONTEXT);
    }
}
