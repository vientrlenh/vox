package com.sep.vox.interfaces.shared;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;

public final class HttpCookieProvider {

    private static final String SAME_SITE_OPTION = "Lax";
    
    public static void setCookie(HttpServletResponse response, String key, String value, long ttl) {
        var cookie = ResponseCookie.from(key, value)
            .httpOnly(true) 
            .secure(false) // http
            .path("/")
            .maxAge(ttl)
            .sameSite(SAME_SITE_OPTION)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void clearCookie(HttpServletResponse response, String key) {
        var cookie = ResponseCookie.from(key, "")
            .httpOnly(true)
            .secure(false) // http
            .path("/")
            .maxAge(0)
            .sameSite(SAME_SITE_OPTION)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
