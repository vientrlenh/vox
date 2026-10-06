package com.sep.vox.infrastructure.security;


import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.infrastructure.properties.RefreshTokenProperties;
import com.sep.vox.infrastructure.shared.TokenHasher;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor 
public class RefreshTokenSessionProvider implements SessionTokenManagerPort {

    private final RefreshTokenProperties refreshTokenProperties;

    private static final SecureRandom sr = new SecureRandom();

    @Override
    public SessionToken generateToken() {
        String seed = refreshTokenProperties.seed();
        int length = refreshTokenProperties.length();
        return createToken(seed, length);
    }

    private SessionToken createToken(String seed, int length) {
        String rawToken = sr.ints(length, 0, seed.length())
            .mapToObj(seed::charAt)
            .map(o -> o.toString())
            .collect(Collectors.joining());
        String hashedToken = hash(rawToken);
        return new SessionToken(rawToken, hashedToken, refreshTokenProperties.expirationMs());
    }

    public String hash(String rawToken) {
        return TokenHasher.sha512(rawToken);
    }
}
