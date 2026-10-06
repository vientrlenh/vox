package com.sep.vox.infrastructure.security;


import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.response.output.SecurePasswordToken;
import com.sep.vox.infrastructure.properties.SecureTokenProperties;
import com.sep.vox.infrastructure.shared.SecureRandomGenerator;
import com.sep.vox.infrastructure.shared.TokenHasher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class SecurePasswordTokenProvider implements PasswordSetUpTokenPort {

    private final SecureTokenProperties secureTokenProperties;

    @Override
    public SecurePasswordToken generateToken() {
        String seed = secureTokenProperties.seed();
        int length = secureTokenProperties.length();
        String rawToken = SecureRandomGenerator.generateRaw(seed, length);
        String hashedToken = hash(rawToken);
        return new SecurePasswordToken(rawToken, hashedToken);
    }

    @Override
    public String hash(String rawToken) {
        return TokenHasher.sha512(rawToken);
    }




   

    
}
