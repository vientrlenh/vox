package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.registerform.RegisterForm;

public interface RegisterFormRepository {
    RegisterForm save(RegisterForm rf);
    Optional<RegisterForm> findById(UUID id);
    PageResult<RegisterForm> findAll(PageRequest pageRequest);
    Optional<RegisterForm> findByIdForUpdate(UUID id);
    int updateApprovedRegisterForm(UUID id, UUID updatedBy, OffsetDateTime now);
    int updateRejectedRegisterForm(UUID id, UUID updatedBy, String reason, OffsetDateTime now);
}
