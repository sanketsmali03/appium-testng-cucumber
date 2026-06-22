package com.eatclub.hooks;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatclub.driver.DriverFactory;
import com.eatclub.driver.DriverManager;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Lifecycle hooks. One fresh driver per scenario (clean state, parallel-safe),
 * torn down afterwards. Also reports pass/fail status to BrowserStack and
 * attaches a screenshot on failure.
 */
public class Hooks {

    private static final Logger log = LoggerFactory.getLogger(Hooks.class);

    @Before(order = 0)
    public void startDriver(Scenario scenario) {
        log.info("Starting scenario: {}", scenario.getName());
        DriverManager.setDriver(DriverFactory.create());
    }

    @After(order = 1)
    public void reportAndScreenshot(Scenario scenario) {
        if (!DriverManager.hasDriver()) {
            return;
        }
        if (scenario.isFailed()) {
            try {
                byte[] png = ((TakesScreenshot) DriverManager.getDriver())
                        .getScreenshotAs(OutputType.BYTES);
                scenario.attach(png, "image/png", scenario.getName());
            } catch (RuntimeException e) {
                log.warn("Could not capture screenshot: {}", e.getMessage());
            }
        }
        markBrowserStackStatus(scenario);
    }

    @After(order = 0)
    public void stopDriver() {
        DriverManager.quitDriver();
    }

    /**
     * Push the scenario verdict to the BrowserStack session via the JS executor
     * annotation hook. Safe no-op when running locally (wrapped in try/catch).
     */
    private void markBrowserStackStatus(Scenario scenario) {
        String status = scenario.isFailed() ? "failed" : "passed";
        String reason = scenario.isFailed()
                ? "Scenario failed: " + scenario.getName()
                : "Scenario passed";
        String payload = String.format(
                "{\"action\": \"setSessionStatus\", \"arguments\": {\"status\":\"%s\", \"reason\": \"%s\"}}",
                status, reason.replace("\"", "'"));
        try {
            ((org.openqa.selenium.JavascriptExecutor) DriverManager.getDriver())
                    .executeScript("browserstack_executor: " + payload);
        } catch (RuntimeException e) {
            log.debug("Skipping BrowserStack status update (not on BrowserStack?): {}", e.getMessage());
        }
    }
}
