package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
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
    public PageResult<RegisterForm> findAll(int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataRegisterFormRepository.findAll(pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(RegisterFormMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public Optional<RegisterForm> findByIdForUpdate(UUID id) {
        return springDataRegisterFormRepository.findByIdForUpdate(id)
            .map(RegisterFormMapper::toDomain);
    }

    @Override
    public int updateApprovedRegisterForm(UUID id, UUID updatedBy, Instant now) {
        return springDataRegisterFormRepository.updateApprovedRegisterForm(id, updatedBy, now);
    }

    @Override
    public int updateRejectedRegisterForm(UUID id, UUID updatedBy, String reason, Instant now) {
        return springDataRegisterFormRepository.updateRejectedRegisterForm(id, updatedBy, reason, now);
    }

    @Override
    public boolean existsBySchoolDirectoryIdAndStatusIn(UUID schoolDirectoryId, Collection<RegisterFormStatus> statuses) {
        return springDataRegisterFormRepository.existsBySchoolDirectoryIdAndStatusIn(schoolDirectoryId, statuses.stream()
            .map(s -> s.name())
            .toList()
        );
    }

    @Override
    public boolean existsByContactEmailAndStatus(String contactEmail, RegisterFormStatus status) {
        return springDataRegisterFormRepository.existsByContactEmailAndStatus(contactEmail, status.name());
    }

    @Override
    public boolean existsByContactPhoneAndStatus(String contactPhone, RegisterFormStatus status) {
        return springDataRegisterFormRepository.existsByContactPhoneAndStatus(contactPhone, status.name());
    }

    @Override
    public boolean existsBySchoolDomainAndStatusIn(String schoolDomain, Collection<RegisterFormStatus> statuses) {
        return springDataRegisterFormRepository.existsBySchoolDomainAndStatusIn(schoolDomain, statuses.stream()
            .map(s -> s.name())
            .toList()
        );
    }
    
    
}
