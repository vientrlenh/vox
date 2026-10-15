package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface SpringDataFrameworkRepository extends JpaRepository<FrameworkJpaEntity, UUID> {
    Optional<FrameworkJpaEntity> findByIdAndStatus(UUID id, String status);
    boolean existsByIdAndStatus(UUID id, String status);

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FrameworkJpaEntity f WHERE f.id = :id AND f.status = :status")
    Optional<FrameworkJpaEntity> findByIdAndStatusForUpdate(@Param("id") UUID id, @Param("status") String status);
}
