package com.sep.vox.domain.model.passwordsetuptoken;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PasswordSetUpToken {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private OffsetDateTime expiredAt;
    private OffsetDateTime usedAt;

    public PasswordSetUpToken() {}

    public PasswordSetUpToken(UUID id, UUID userId, String tokenHash, OffsetDateTime expiredAt, OffsetDateTime usedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
    }

    
}
