package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.infrastructure.persistence.mapper.RoleMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRoleRepository;

@Repository
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
    public Optional<Role> findById(UUID id) {
        return springDataRoleRepository.findById(id)
            .map(RoleMapper::toDomain);
    }

    @Override
    public List<Role> findByName(String name) {
        return springDataRoleRepository.findByName(name)
            .stream()
            .map(RoleMapper::toDomain)
            .toList();
    }

    @Override
    public Role save(Role role) {
        var entity = RoleMapper.toJpa(role);
        var saved = springDataRoleRepository.save(entity);
        return RoleMapper.toDomain(saved);
    }

    @Override
    public long count() {
        return springDataRoleRepository.count();
    }

    @Override
    public List<Role> findByIdIn(Collection<UUID> ids) {
        return springDataRoleRepository.findByIdIn(ids)
            .stream()
            .map(RoleMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<Role> findAll(int page, int size) {
        var pageRequest = PageRequest.of(page - 1, size);
        var pageable = springDataRoleRepository.findAll(pageRequest);
        return new PageResult<>(
            pageable.getContent()
                .stream()
                .map(RoleMapper::toDomain)
                .toList(),
            page, 
            size, 
            pageable.getTotalElements(), 
            pageable.getTotalPages()
        );
    }

    
}
