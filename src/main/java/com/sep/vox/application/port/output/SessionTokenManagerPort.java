package com.sep.vox.application.port.output;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.response.output.SessionToken;

public interface SessionTokenManagerPort {
    SessionToken generateToken();
    String hash(String rawToken);
}
