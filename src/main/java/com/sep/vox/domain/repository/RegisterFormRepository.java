package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.registerform.RegisterForm;

public interface RegisterFormRepository {
    RegisterForm save(RegisterForm rf);
    Optional<RegisterForm> findById(UUID id);
}
