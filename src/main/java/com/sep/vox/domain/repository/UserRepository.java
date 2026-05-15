package com.sep.vox.domain.repository;

import java.util.Optional;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.valueobject.id.UserId;

public interface UserRepository {
    Optional<User> findById(UserId id);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    User save(User user);
}
