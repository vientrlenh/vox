package com.sep.vox.domain.repository;

import java.util.List;

import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.valueobject.id.RoleId;
import com.sep.vox.domain.valueobject.id.UserId;

public interface UserRoleRepository {
    List<UserRole> findByRoleId(RoleId id);
    List<UserRole> findByUserId(UserId id);
}
