package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;

public interface RefreshTokenRepository {
    List<RefreshToken> findBySessionId(UUID sessionId);
    Optional<RefreshToken> findById(UUID id);
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
