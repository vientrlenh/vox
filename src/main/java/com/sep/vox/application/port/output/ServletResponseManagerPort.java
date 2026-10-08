package com.sep.vox.application.port.output;

import jakarta.servlet.http.HttpServletResponse;

public interface ServletResponseManagerPort {
    void setHttpOnlyCookie(HttpServletResponse res, String key, String value, long ttl);
    void clearHttpOnlyCookie(HttpServletResponse res, String key);
}
