package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;

import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.valueobject.id.RoleId;

public interface RoleRepository {
    Optional<Role> findByCode(String code);
    Optional<Role> findById(RoleId id);
    List<Role> findByName(String name);
}
