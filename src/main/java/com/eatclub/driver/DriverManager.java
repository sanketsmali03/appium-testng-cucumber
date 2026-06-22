package com.eatclub.driver;

import io.appium.java_client.AppiumDriver;

/**
 * Thread-safe holder of the active {@link AppiumDriver}.
 *
 * Cucumber + TestNG can run scenarios in parallel; each thread gets its own
 * driver instance. Never expose a static driver field directly — always go
 * through this ThreadLocal so parallel scenarios don't clobber each other.
 */
public final class DriverManager {

    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static AppiumDriver getDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "Driver not initialised on thread '" + Thread.currentThread().getName()
                            + "'. Did the @Before hook run?");
        }
        return driver;
    }

    public static void setDriver(AppiumDriver driver) {
        DRIVER.set(driver);
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static void quitDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }
}
