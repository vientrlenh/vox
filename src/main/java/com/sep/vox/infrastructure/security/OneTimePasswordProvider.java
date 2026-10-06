package com.sep.vox.infrastructure.security;


import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.infrastructure.properties.OtpProperties;
import com.sep.vox.infrastructure.shared.SecureRandomGenerator;
import com.sep.vox.infrastructure.shared.TokenHasher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class OneTimePasswordProvider implements OneTimePasswordPort {

    private final OtpProperties otpProperties;

    @Override
    public String generateOtp(int size) {
        String seed = otpProperties.seed();
        return SecureRandomGenerator.generateRaw(seed, size);
    }

    @Override
    public String hash(String otp) {
        return TokenHasher.sha256(otp);
    }
    
}
