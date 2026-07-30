package com.sep.vox.application.port.output;

import java.time.Instant;
import java.util.UUID;

public interface SessionManagerPort {
    void revoke(UUID sessionId, Instant now);
}
