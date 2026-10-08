package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.shared.PageResult;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByIdForUpdate(UUID id);
    List<User> findByIdIn(Collection<UUID> ids);
    List<User> findByEmailIn(Collection<String> emails);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    // Replace the find with status method, mostly used with active state
    Optional<User> findByIdAndStatusActive(UUID id); 

    User save(User user);
    User saveAndFlush(User user);
    boolean existsByEmail(String email);

    boolean existsByEmailAndStatusActive(String email);
    int changeUserPassword(String email, String passwordHash);
    boolean existsByIdAndStatus(UUID id, UserStatus status);
    boolean existsByPhone(String phone);
    PageResult<User> findAll(int pageNumber, int size);
}
