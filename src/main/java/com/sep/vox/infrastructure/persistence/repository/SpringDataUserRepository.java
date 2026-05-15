package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID>{
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByPhone(String phone);
}
