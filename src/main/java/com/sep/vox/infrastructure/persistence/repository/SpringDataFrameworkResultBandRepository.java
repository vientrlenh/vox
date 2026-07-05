package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SpringDataFrameworkResultBandRepository extends JpaRepository<FrameworkResultBandJpaEntity, UUID> {
    List<FrameworkResultBandJpaEntity> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkResultBandJpaEntity> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);
    Optional<FrameworkResultBandJpaEntity> findByFrameworkVersionIdAndCode(UUID frameworkVersionId, String code);
    Optional<FrameworkResultBandJpaEntity> findByFrameworkVersionIdAndLabel(UUID frameworkVersionId, String label);

    @Modifying
    @Query("DELETE FROM FrameworkResultBandJpaEntity b WHERE b.frameworkVersionId = :frameworkVersionId")
    void deleteByFrameworkVersionId(@Param("frameworkVersionId") UUID frameworkVersionId);
}
