package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    User save(User user);
    boolean existsByEmail(String email);
    boolean existsByEmailAndStatus(String email, UserStatus status);
    int changeUserPassword(String email, String passwordHash);
}
