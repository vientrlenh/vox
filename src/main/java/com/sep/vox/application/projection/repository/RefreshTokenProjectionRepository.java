package com.sep.vox.application.projection.repository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenProjectionRepository {
    Optional<UUID> findDeviceSessionIdByTokenHash(String tokenHash);
}
