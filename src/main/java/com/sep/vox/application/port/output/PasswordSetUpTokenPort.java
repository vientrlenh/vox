package com.sep.vox.application.port.output;

import com.sep.vox.application.response.output.GeneratedPasswordSetUpToken;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;

public interface PasswordSetUpTokenPort {
    GeneratedPasswordSetUpToken generateToken();
    boolean compare(String rawToken, PasswordSetUpToken passwordSetUpToken);
}
