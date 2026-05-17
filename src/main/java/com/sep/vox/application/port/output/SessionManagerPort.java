package com.sep.vox.application.port.output;

import java.util.UUID;

public interface SessionManagerPort {
    String setSessionAndGetRefreshTokenWhenLogin(UUID userId);
    boolean compareMatchesToken(String raw, UUID requestedUserId);
}
