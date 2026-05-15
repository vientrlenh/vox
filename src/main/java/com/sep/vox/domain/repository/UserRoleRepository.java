package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.userrole.UserRole;

public interface UserRoleRepository {
    List<UserRole> findByRoleId(UUID id);
    List<UserRole> findByUserId(UUID id);
    UserRole save(UserRole userRole);
}
