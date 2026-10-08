package com.sep.vox.infrastructure.security;

import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.ServletRequestManagerPort;
import com.sep.vox.application.port.output.ServletResponseManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.infrastructure.properties.RefreshTokenProperties;
import com.sep.vox.infrastructure.shared.SecureRandomGenerator;
import com.sep.vox.infrastructure.shared.TokenHasher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor 
public class RefreshTokenSessionProvider implements SessionTokenManagerPort {

    private final RefreshTokenProperties refreshTokenProperties;
    private final ServletRequestManagerPort servletRequestManagerPort;
    private final ServletResponseManagerPort servletResponseManagerPort;

    private static final String COOKIE_KEY = "refresh_token";

    @Override
    public SessionToken generateToken(HttpServletResponse res) {
        String seed = refreshTokenProperties.seed();
        int length = refreshTokenProperties.length();
        SessionToken token = createToken(seed, length);
        storeRefreshToken(res, token.rawToken());
        return token;
    }

    private SessionToken createToken(String seed, int length) {
        String rawToken = SecureRandomGenerator.generateRaw(seed, length);
        String hashedToken = hash(rawToken);
        return new SessionToken(
            rawToken, 
            hashedToken, 
            refreshTokenProperties.expirationMs()
        );
    }

    public String hash(String rawToken) {
        return TokenHasher.sha512(rawToken);
    }

    private void storeRefreshToken(HttpServletResponse res, String rawToken) {
        long ttlSeconds = refreshTokenProperties.expirationMs() * 1000;
        servletResponseManagerPort.setHttpOnlyCookie(res, COOKIE_KEY, rawToken, ttlSeconds);
    }

    @Override
    public String getRawFromCookie(HttpServletRequest req) {
        return servletRequestManagerPort.getCookie(req, COOKIE_KEY)
            .orElseThrow(() -> new UnauthorizedException("Token not found"));
    }
}
