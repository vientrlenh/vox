package com.sep.vox.application.port.output;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SessionManagerPort {
    void revoke(UUID sessionId, OffsetDateTime now);
}
