package com.sep.vox.infrastructure.security;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.ServletRequestManagerPort;
import com.sep.vox.application.port.output.ServletResponseManagerPort;
import com.sep.vox.infrastructure.properties.HttpOnlyCookieProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class HttpServletContextProvider implements ServletRequestManagerPort, ServletResponseManagerPort {

    private final HttpOnlyCookieProperties cookieProperties;

    @Override
    public void setHttpOnlyCookie(HttpServletResponse res, String key, String value, long ttl) {
        ResponseCookie cookie = ResponseCookie.from(key, value)
            .httpOnly(true)
            .secure(cookieProperties.secure())
            .path("/")
            .maxAge(ttl)
            .sameSite(cookieProperties.sameSite())
            .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public void clearHttpOnlyCookie(HttpServletResponse res, String key) {
        setHttpOnlyCookie(res, key, "", 0);
    }

    @Override
    public void setSession(HttpServletRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSession'");
    }

    @Override
    public Optional<String> getCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            String cookieValue = Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(cookie -> cookie.getValue())
                .findFirst()
                .orElse(null);
            if (cookieValue == null) {
                return Optional.empty();
            }
            return Optional.of(cookieValue);
        }   
        return Optional.empty();
    }

	@Override
	public String getContextIpAddress(HttpServletRequest req) {
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

	@Override
	public String getUserAgent(HttpServletRequest req) {
		return req.getHeader("User-Agent");
	}
}
