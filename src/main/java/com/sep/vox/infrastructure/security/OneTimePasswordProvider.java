package com.sep.vox.infrastructure.security;


import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.OneTimePasswordPort;

@Component
public class OneTimePasswordProvider implements OneTimePasswordPort {

    private final SecureTokenProvider secureTokenProvider;

    public OneTimePasswordProvider(SecureTokenProvider secureTokenProvider) {
        this.secureTokenProvider = secureTokenProvider;
    }

    @Override
    public String generate(int size) {
        return secureTokenProvider.generateDigits(size);
    }

    @Override
    public String hash(String otp) {
        return secureTokenProvider.sha256(otp);
    }

    
}
