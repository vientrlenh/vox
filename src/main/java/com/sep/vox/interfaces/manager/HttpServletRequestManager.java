package com.sep.vox.interfaces.manager;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component 
public class HttpServletRequestManager {
    
    public void clearOAuth2Session(HttpSession session) {
        session.removeAttribute("oauth2_device_id");
        session.removeAttribute("oauth2_device_name");
        session.removeAttribute("oauth2_platform");
        session.invalidate();
    }

	public String getIpAddress(HttpServletRequest req) {
		String forwardedFor = req.getHeader("X-Forwarded-For");
        if (clientIpHasValue(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (clientIpHasValue(realIp)) {
            return realIp.trim();
        }
        return req.getRemoteAddr();
	}
    
    private boolean clientIpHasValue(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value);
    }

	public String getUserAgent(HttpServletRequest req) {
		return req.getHeader("User-Agent");
	}
}
