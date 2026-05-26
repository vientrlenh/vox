package com.sep.vox.application.port.output;

import com.sep.vox.application.response.output.GeneratedPasswordSetUpToken;

public interface PasswordSetUpTokenPort {
    GeneratedPasswordSetUpToken generateToken();
    String hash(String rawToken);
}
