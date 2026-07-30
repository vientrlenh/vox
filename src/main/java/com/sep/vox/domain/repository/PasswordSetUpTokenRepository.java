package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;

public interface PasswordSetUpTokenRepository {
    Optional<PasswordSetUpToken> findById(UUID id);
    Optional<PasswordSetUpToken> findByUserIdAndTokenHash(UUID userId, String tokenHash);
    PasswordSetUpToken save(PasswordSetUpToken passwordSetUpToken);
    int updateUsedToken(UUID userId, String tokenHash, Instant now);
}
