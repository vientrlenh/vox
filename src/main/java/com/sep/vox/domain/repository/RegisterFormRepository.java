package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;

public interface RegisterFormRepository {
    RegisterForm save(RegisterForm rf);
    Optional<RegisterForm> findById(UUID id);
    PageResult<RegisterForm> findAll(int pageNumber, int size);
    Optional<RegisterForm> findByIdForUpdate(UUID id);
    int updateApprovedRegisterForm(UUID id, UUID updatedBy, Instant now);
    int updateRejectedRegisterForm(UUID id, UUID updatedBy, String reason, Instant now);
    boolean existsBySchoolDirectoryIdAndStatusIn(UUID schoolDirectoryId, Collection<RegisterFormStatus> statuses);
    boolean existsByContactEmailAndStatus(String contactEmail, RegisterFormStatus status);
    boolean existsByContactPhoneAndStatus(String contactPhone, RegisterFormStatus status);

    long countByStatus(RegisterFormStatus status);
    long countByCreatedAtAfter(Instant after);
}
