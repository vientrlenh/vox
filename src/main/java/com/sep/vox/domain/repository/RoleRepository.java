package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.role.Role;

public interface RoleRepository {
    Optional<Role> findByCode(String code);
    Optional<Role> findById(UUID id);
    List<Role> findByName(String name);
    Role save(Role role);
    long count();
}
