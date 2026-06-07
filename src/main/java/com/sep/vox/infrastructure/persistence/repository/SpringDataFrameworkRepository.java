package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;

public interface SpringDataFrameworkRepository extends JpaRepository<FrameworkJpaEntity, UUID> {
    Optional<FrameworkJpaEntity> findByCode(String code);

    @Modifying
    @Query("UPDATE FrameworkJpaEntity f SET f.currentVersionId = :currentVersionId WHERE f.id = :id")
    int updateCurrentVersionId(@Param("id") UUID id, @Param("currentVersionId") UUID currentVersionId);
}
