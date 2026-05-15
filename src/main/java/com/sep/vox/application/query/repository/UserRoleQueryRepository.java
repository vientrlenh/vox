package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.UserRoleInfo;

public interface UserRoleQueryRepository {
    List<UserRoleInfo> findByUserIdWithRoleInfo(UUID userId);
}
