package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.user.UserRole;

public interface UserRoleRepository {
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);
    List<UserRole> findByRoleId(UUID roleId);
    List<UserRole> findByUserId(UUID userId);
    UserRole save(UserRole userRole);
    boolean existsByRoleId(UUID roleId);

    List<UserRole> findByUserIdIn(Collection<UUID> userIds);
    List<UserRole> findByRoleIdIn(Collection<UUID> roleIds, int pageNumber, int size);
    long countByRoleId(UUID roleId);
}
