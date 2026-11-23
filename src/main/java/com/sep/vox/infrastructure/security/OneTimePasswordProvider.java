package com.sep.vox.infrastructure.security;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.OneTimePasswordPort;

@Component
@RequiredArgsConstructor
public class OneTimePasswordProvider implements OneTimePasswordPort {

    private final SecureTokenProvider secureTokenProvider;

    @Override
    public String generate(int size) {
        return secureTokenProvider.generateDigits(size);
    }

    @Override
    public String hash(String otp) {
        return secureTokenProvider.sha256(otp);
    }

    
}
