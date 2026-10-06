package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;

public interface RefreshTokenRepository {
    List<RefreshToken> findByDeviceSessionId(UUID sessionId);
    Optional<RefreshToken> findById(UUID id);
    RefreshToken save(RefreshToken token);
    boolean existsByTokenHash(String tokenHash);
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);
    int markUsedAndReplacedBy(UUID oldTokenId, UUID newTokenId, Instant now);
}
