package com.sep.vox.interfaces.manager;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.sep.vox.interfaces.properties.HttpOnlyCookieProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class HttpServletResponseManager {
    
    private final HttpOnlyCookieProperties httpOnlyCookieProperties;

    public void setHttpOnlyCookie(HttpServletResponse response, String key, String value, long ttl) {
        ResponseCookie cookie = ResponseCookie.from(key, value)
            .httpOnly(true)
            .secure(httpOnlyCookieProperties.secure())
            .path("/")
            .maxAge(ttl)
            .sameSite(httpOnlyCookieProperties.sameSite())
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearHttpOnlyCookie(HttpServletResponse res, String key) {
        setHttpOnlyCookie(res, key, "", 0);
    }
}
