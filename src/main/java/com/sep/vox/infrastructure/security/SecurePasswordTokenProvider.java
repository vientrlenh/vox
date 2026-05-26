package com.sep.vox.infrastructure.security;


import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.response.output.GeneratedPasswordSetUpToken;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;

@Component
public class SecurePasswordTokenProvider implements PasswordSetUpTokenPort {

    private final SecureTokenProvider secureTokenProvider;

    public SecurePasswordTokenProvider(SecureTokenProvider secureTokenProvider) {
        this.secureTokenProvider = secureTokenProvider;
    }

    private static final int TOKEN_LENGTH = 32;

    @Override
    public GeneratedPasswordSetUpToken generateToken() {
        var token = secureTokenProvider.generateToken(TOKEN_LENGTH);
        var hashedToken = secureTokenProvider.sha512(token);
        return new GeneratedPasswordSetUpToken(token, hashedToken);
    }

    @Override
    public boolean compare(String rawToken, PasswordSetUpToken passwordSetUpToken) {
        if (passwordSetUpToken == null) {
            return false;
        }
        var hashedToken = secureTokenProvider.sha512(rawToken);
        return hashedToken.equals(passwordSetUpToken.getTokenHash());
    }

   

    
}
