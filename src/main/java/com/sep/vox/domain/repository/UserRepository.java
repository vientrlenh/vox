package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByIdForUpdate(UUID id);
    List<User> findByIdIn(Collection<UUID> ids);
    List<User> findByEmailIn(Collection<String> emails);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndStatus(String email, UserStatus status);
    Optional<User> findByIdAndStatus(UUID id, UserStatus status);
    User save(User user);
    boolean existsByEmail(String email);
    boolean existsByEmailAndStatus(String email, UserStatus status);
    int changeUserPassword(String email, String passwordHash);
    boolean existsByIdAndStatus(UUID id, UserStatus status);
    boolean existsByPhone(String phone);
    PageResult<User> findAll(int page, int size);
}
