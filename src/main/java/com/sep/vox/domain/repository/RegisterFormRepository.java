package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.util.PageRequest;
import com.sep.vox.domain.util.PageResult;

public interface RegisterFormRepository {
    RegisterForm save(RegisterForm rf);
    Optional<RegisterForm> findById(UUID id);
    PageResult<RegisterForm> findAll(PageRequest pageRequest);
}
