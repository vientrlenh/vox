package com.sep.vox.infrastructure.security;


import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.response.output.GeneratedSessionToken;

@Component
public class RefreshTokenSessionProvider implements SessionManagerPort {

    private final SecureTokenProvider secureTokenProvider;

    public RefreshTokenSessionProvider(SecureTokenProvider secureTokenprovider) {
        this.secureTokenProvider = secureTokenprovider;
    }

    private static final int TOKEN_LENGTH = 32;
    
    @Override
    public GeneratedSessionToken generateToken() {
        var token = secureTokenProvider.generateToken(TOKEN_LENGTH);
        var hashedToken = secureTokenProvider.sha512(token);
        return new GeneratedSessionToken(token, hashedToken);
    }

    
}
