package com.sep.vox.application.port.output;

import com.sep.vox.application.response.output.SessionToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SessionTokenManagerPort {
    SessionToken generateToken(HttpServletResponse res);
    String hash(String rawToken);
    String getRawFromCookie(HttpServletRequest req);
}
