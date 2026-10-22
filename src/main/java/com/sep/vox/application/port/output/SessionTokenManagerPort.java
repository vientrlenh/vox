package com.sep.vox.application.port.output;

import com.sep.vox.application.response.output.SessionToken;

public interface SessionTokenManagerPort {
    SessionToken generateToken();
    String hash(String rawToken);
}
