package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.infrastructure.persistence.mapper.UserRoleMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataUserRoleRepository;

@Repository
public class UserRoleRepositoryImpl implements UserRoleRepository {

    private final SpringDataUserRoleRepository springDataUserRoleRepository;

    public UserRoleRepositoryImpl(SpringDataUserRoleRepository springDataUserRoleRepository) {
        this.springDataUserRoleRepository = springDataUserRoleRepository;
    }

    @Override
    public List<UserRole> findByRoleId(UUID id) {
        return springDataUserRoleRepository.findByRoleId(id)
            .stream()
            .map(UserRoleMapper::toDomain)
            .toList();
    }

    @Override
    public List<UserRole> findByUserId(UUID id) {
        return springDataUserRoleRepository.findByUserId(id)
            .stream()
            .map(UserRoleMapper::toDomain)
            .toList();
    }

    @Override
    public UserRole save(UserRole userRole) {
        var entity = UserRoleMapper.toJpa(userRole);
        var saved = springDataUserRoleRepository.save(entity);
        return UserRoleMapper.toDomain(saved);
    }

    @Override
    public boolean existsByRoleId(UUID roleId) {
        return springDataUserRoleRepository.existsByRoleId(roleId);
    }
    
}
