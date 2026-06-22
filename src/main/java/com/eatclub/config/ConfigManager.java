package com.eatclub.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.eatclub.enums.Platform;

/**
 * Loads execution configuration with a clear precedence:
 *   1. System property  (-Dkey=value)
 *   2. Environment variable (KEY)
 *   3. Per-platform properties file on the classpath (config/local-android.properties, etc.)
 *
 * Only the LOCAL execution path needs these (Appium server URL, app path, caps).
 * On BrowserStack the SDK injects capabilities from browserstack.yml, so the
 * local properties are not required.
 */
public final class ConfigManager {

    private static final String BROWSERSTACK = "browserstack";

    private final Properties properties = new Properties();

    private ConfigManager(Platform platform) {
        String resource = "config/local-" + platform.name().toLowerCase() + ".properties";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resource)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config: " + resource, e);
        }
    }

    public static ConfigManager forPlatform(Platform platform) {
        return new ConfigManager(platform);
    }

    /** Where tests run: "browserstack" (default) or "local". */
    public static String executionTarget() {
        return resolve("execution.target", "EXECUTION_TARGET", BROWSERSTACK);
    }

    public static boolean isBrowserStack() {
        return BROWSERSTACK.equalsIgnoreCase(executionTarget());
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        String fromOverride = resolve(key, toEnvKey(key), null);
        if (fromOverride != null) {
            return fromOverride;
        }
        return properties.getProperty(key, defaultValue);
    }

    /** System property -> env var -> default. */
    private static String resolve(String sysProp, String envVar, String defaultValue) {
        String value = System.getProperty(sysProp);
        if (value == null || value.isEmpty()) {
            value = System.getenv(envVar);
        }
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase().replace('.', '_');
    }
}
