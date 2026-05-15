package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.user.User;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    User save(User user);
}
