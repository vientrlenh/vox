package com.sep.vox.application.projection.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.projection.dto.UserRoleInfo;

public interface UserRoleQueryRepository {
    List<UserRoleInfo> findByUserIdWithRoleInfo(UUID userId);
    Set<UUID> findUserIdsByRoleCode(Collection<UUID> userIds, String roleCode);
}
