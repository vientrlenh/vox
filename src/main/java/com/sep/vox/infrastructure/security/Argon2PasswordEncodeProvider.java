package com.sep.vox.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.sep.vox.application.port.output.PasswordEncoderPort;

public class Argon2PasswordEncodeProvider implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public Argon2PasswordEncodeProvider(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String raw, String hashed) {
        return passwordEncoder.matches(raw, hashed);
    }
    

}
