package com.sep.vox.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.CookieManagerPort;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class HttpCookieProvider implements CookieManagerPort {
    
    private final boolean secure;
    private final String sameSite;

    public HttpCookieProvider(@Value("${app.cookie.secure}") boolean secure, @Value("${app.cookie.same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    @Override
    public void setCookie(HttpServletResponse response, String key, String value, long ttl) {
        var cookie = ResponseCookie.from(key, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(ttl)
            .sameSite(sameSite)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    
}
