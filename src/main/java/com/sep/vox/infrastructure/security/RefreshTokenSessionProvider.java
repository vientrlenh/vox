package com.sep.vox.infrastructure.security;


import org.springframework.stereotype.Component;


import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;


@Component
public class RefreshTokenSessionProvider implements SessionTokenManagerPort {

    private final SecureTokenProvider secureTokenProvider;


    public RefreshTokenSessionProvider(SecureTokenProvider secureTokenprovider, DeviceSessionRepository deviceSessionRepository) {
        this.secureTokenProvider = secureTokenprovider;
    }

    private static final int TOKEN_LENGTH = 32;
    
    @Override
    public GeneratedSessionToken generateToken() {
        var token = secureTokenProvider.generateToken(TOKEN_LENGTH);
        var hashedToken = secureTokenProvider.sha512(token);
        return new GeneratedSessionToken(token, hashedToken);
    }

    @Override
    public String hash(String token) {
        return secureTokenProvider.sha512(token);
    }


}
