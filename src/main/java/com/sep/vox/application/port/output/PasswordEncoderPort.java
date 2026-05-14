package com.sep.vox.application.port.output;

public interface PasswordEncoderPort {
    String hash(String rawPassword);
    boolean matches(String raw, String hashed);
}
