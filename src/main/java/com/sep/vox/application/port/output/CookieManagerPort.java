package com.sep.vox.application.port.output;

import jakarta.servlet.http.HttpServletResponse;

public interface CookieManagerPort {
    void setCookie(HttpServletResponse response, String key, String value, long ttl);
    void clearCookie(HttpServletResponse response, String key);
}
