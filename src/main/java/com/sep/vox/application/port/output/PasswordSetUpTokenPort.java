package com.sep.vox.application.port.output;

import com.sep.vox.application.response.output.SecurePasswordToken;

public interface PasswordSetUpTokenPort {
    SecurePasswordToken generateToken();
    String hash(String rawToken);
}
