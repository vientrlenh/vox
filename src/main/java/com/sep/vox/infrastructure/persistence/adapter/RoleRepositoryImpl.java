package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.valueobject.id.RoleId;
import com.sep.vox.infrastructure.persistence.mapper.RoleMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRoleRepository;

public class RoleRepositoryImpl implements RoleRepository {

    private final SpringDataRoleRepository springDataRoleRepository;

    public RoleRepositoryImpl(SpringDataRoleRepository springDataRoleRepository) {
        this.springDataRoleRepository = springDataRoleRepository;
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return springDataRoleRepository.findByCode(code)
            .map(RoleMapper::toDomain);
    }

    @Override
    public Optional<Role> findById(RoleId id) {
        return springDataRoleRepository.findById(id.value())
            .map(RoleMapper::toDomain);
    }

    @Override
    public List<Role> findByName(String name) {
        return springDataRoleRepository.findByName(name)
            .stream()
            .map(RoleMapper::toDomain)
            .toList();
    }
    
}
