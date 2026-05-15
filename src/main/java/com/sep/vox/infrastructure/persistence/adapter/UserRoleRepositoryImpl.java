package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;

import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.id.RoleId;
import com.sep.vox.domain.valueobject.id.UserId;
import com.sep.vox.infrastructure.persistence.mapper.UserRoleMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataUserRoleRepository;

public class UserRoleRepositoryImpl implements UserRoleRepository {

    private final SpringDataUserRoleRepository springDataUserRoleRepository;

    public UserRoleRepositoryImpl(SpringDataUserRoleRepository springDataUserRoleRepository) {
        this.springDataUserRoleRepository = springDataUserRoleRepository;
    }

    @Override
    public List<UserRole> findByRoleId(RoleId id) {
        return springDataUserRoleRepository.findByRoleId(id.value())
            .stream()
            .map(UserRoleMapper::toDomain)
            .toList();
    }

    @Override
    public List<UserRole> findByUserId(UserId id) {
        return springDataUserRoleRepository.findByUserId(id.value())
            .stream()
            .map(UserRoleMapper::toDomain)
            .toList();
    }
    
}
