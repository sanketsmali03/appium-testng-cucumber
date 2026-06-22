package com.eatclub.enums;

/**
 * Supported mobile platforms. Resolved from the {@code -Dplatform} system
 * property (set by the Maven android/ios profiles).
 */
public enum Platform {
    ANDROID,
    IOS;

    public static Platform current() {
        String value = System.getProperty("platform", "android").trim().toLowerCase();
        switch (value) {
            case "ios":
                return IOS;
            case "android":
                return ANDROID;
            default:
                throw new IllegalArgumentException("Unsupported platform: '" + value
                        + "'. Use -Dplatform=android or -Dplatform=ios.");
        }
    }

    public boolean isAndroid() {
        return this == ANDROID;
    }

    public boolean isIOS() {
        return this == IOS;
    }
}
