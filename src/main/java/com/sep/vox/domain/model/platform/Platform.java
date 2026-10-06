package com.sep.vox.domain.model.platform;

public enum Platform {
    WEB,
    IOS,
    ANDROID,
    DESKTOP;

    public static Platform from(String platform) {
        return platform == null ? null : Platform.valueOf(platform);
    }

    public static String value(Platform platform) {
        return platform == null ? null : platform.name();
    }
}
