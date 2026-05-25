package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.infrastructure.persistence.mapper.RegisterFormMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRegisterFormRepository;

@Repository
public class RegisterFormRepositoryImpl implements RegisterFormRepository {

    private final SpringDataRegisterFormRepository springDataRegisterFormRepository;

    public RegisterFormRepositoryImpl(SpringDataRegisterFormRepository springDataRegisterFormRepository) {
        this.springDataRegisterFormRepository = springDataRegisterFormRepository;
    }

    @Override
    public RegisterForm save(RegisterForm rf) {
        var entity = RegisterFormMapper.toJpa(rf);
        var saved = springDataRegisterFormRepository.save(entity);
        return RegisterFormMapper.toDomain(saved);
    }

    @Override
    public Optional<RegisterForm> findById(UUID id) {
        return springDataRegisterFormRepository.findById(id)
            .map(RegisterFormMapper::toDomain);
    }

    @Override
    public PageResult<RegisterForm> findAll(PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        var page = springDataRegisterFormRepository.findAll(pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(RegisterFormMapper::toDomain)
                .toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
    
}
