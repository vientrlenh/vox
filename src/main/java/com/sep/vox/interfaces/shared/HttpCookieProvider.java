package com.sep.vox.interfaces.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class HttpCookieProvider {
    
    private final boolean secure;
    private final String sameSite;

    public HttpCookieProvider(@Value("${app.cookie.secure}") boolean secure, @Value("${app.cookie.same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void setCookie(HttpServletResponse response, String key, String value, long ttl) {
        ResponseCookie cookie = ResponseCookie.from(key, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(ttl)
            .sameSite(sameSite)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Clears the cookie by writing it again with maxAge = 0.
     *
     * <p>It deliberately calls {@link #setCookie} instead of building a separate {@link ResponseCookie}: the
     * browser only overwrites a cookie when the identifying attributes (path, domain, secure, sameSite) match
     * the ones used when it was set EXACTLY. If one differs, the browser treats it as another cookie, the old one
     * stays in place and the refresh token keeps working -- a failure that leaves no trace on the server, because
     * the response is still 200 with a perfectly valid-looking Set-Cookie. Going through a single method means
     * the two attribute sets cannot drift apart.
     */
    public void clearCookie(HttpServletResponse response, String key) {
        setCookie(response, key, "", 0);
    }
}
