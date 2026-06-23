package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.user.Role;

public interface RoleRepository {
    Optional<Role> findByCode(String code);

    Optional<Role> findById(UUID id);

    List<Role> findByName(String name);

    Role save(Role role);

    long count();

    List<Role> findByIdIn(Collection<UUID> ids);

    List<Role> findByCodeIn(Collection<String> codes);

    PageResult<Role> findAll(int page, int size);
}
