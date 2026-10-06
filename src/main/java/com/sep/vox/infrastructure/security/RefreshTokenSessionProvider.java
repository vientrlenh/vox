package com.sep.vox.infrastructure.security;

import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.infrastructure.properties.RefreshTokenProperties;
import com.sep.vox.infrastructure.shared.SecureRandomGenerator;
import com.sep.vox.infrastructure.shared.TokenHasher;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor 
public class RefreshTokenSessionProvider implements SessionTokenManagerPort {

    private final RefreshTokenProperties refreshTokenProperties;

    @Override
    public SessionToken generateToken() {
        String seed = refreshTokenProperties.seed();
        int length = refreshTokenProperties.length();
        return createToken(seed, length);
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
}
