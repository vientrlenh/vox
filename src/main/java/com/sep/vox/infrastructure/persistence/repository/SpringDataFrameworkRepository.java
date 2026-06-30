package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface SpringDataFrameworkRepository extends JpaRepository<FrameworkJpaEntity, UUID> {
    Optional<FrameworkJpaEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FrameworkJpaEntity f WHERE f.id = :id")
    Optional<FrameworkJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT f FROM FrameworkJpaEntity f WHERE " +
           "(:search IS NULL OR LOWER(f.name) LIKE :search ESCAPE '!' OR LOWER(f.code) LIKE :search ESCAPE '!')")
    Page<FrameworkJpaEntity> findAllBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT f FROM FrameworkJpaEntity f WHERE " +
           "(:search IS NULL OR LOWER(f.name) LIKE :search ESCAPE '!' OR LOWER(f.code) LIKE :search ESCAPE '!') " +
           "AND f.isActive = :isActive")
    Page<FrameworkJpaEntity> findAllBySearchAndIsActive(@Param("search") String search, @Param("isActive") boolean isActive, Pageable pageable);
}
