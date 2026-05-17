package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.session.Session;

public interface SessionRepository {
    Session save(Session session);
    Optional<Session> findByUserIdAndRefreshTokenHash(UUID userId, String refreshTokenHash);
}
