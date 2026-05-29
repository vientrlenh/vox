package com.sep.vox.interfaces.shared;


import jakarta.servlet.http.HttpServletRequest;

public final class IpAddressReceiver {

    public static String getClientIp(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (hasValue(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        
        var realIp = request.getHeader("X-Real-IP");
        if (hasValue(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value);
    }
}
