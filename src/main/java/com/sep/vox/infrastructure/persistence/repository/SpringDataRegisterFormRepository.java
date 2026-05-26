package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.sep.vox.infrastructure.persistence.entity.RegisterFormJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataRegisterFormRepository extends JpaRepository<RegisterFormJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegisterFormJpaEntity r WHERE r.id = :id")
    Optional<RegisterFormJpaEntity> findByIdForUpdate(UUID id);
}
