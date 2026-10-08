package com.sep.vox.application.port.output;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

public interface ServletRequestManagerPort {
    Optional<String> getCookie(HttpServletRequest req, String key);
    void setSession(HttpServletRequest req);
    String getContextIpAddress(HttpServletRequest req);
    String getUserAgent(HttpServletRequest req);
}
