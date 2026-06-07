package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;

public interface SpringDataFrameworkResultBandRepository extends JpaRepository<FrameworkResultBandJpaEntity, UUID> {
    List<FrameworkResultBandJpaEntity> findByFrameworkVersionId(UUID frameworkVersionId);

    @Modifying
    @Query("DELETE FROM FrameworkResultBandJpaEntity b WHERE b.frameworkVersionId = :frameworkVersionId")
    void deleteByFrameworkVersionId(@Param("frameworkVersionId") UUID frameworkVersionId);
}
