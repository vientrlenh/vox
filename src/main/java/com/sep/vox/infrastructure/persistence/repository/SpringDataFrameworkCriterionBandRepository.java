package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionBandJpaEntity;

public interface SpringDataFrameworkCriterionBandRepository extends JpaRepository<FrameworkCriterionBandJpaEntity, UUID> {
    List<FrameworkCriterionBandJpaEntity> findByFrameworkCriterionIdIn(Collection<UUID> frameworkCriterionIds);

    @Modifying
    @Query("DELETE FROM FrameworkCriterionBandJpaEntity b WHERE b.frameworkCriterionId IN :frameworkCriterionIds")
    void deleteByFrameworkCriterionIdIn(@Param("frameworkCriterionIds") Collection<UUID> frameworkCriterionIds);

    @Modifying
    @Query("DELETE FROM FrameworkCriterionBandJpaEntity b WHERE b.frameworkCriterionId IN (SELECT c.id FROM FrameworkCriterionJpaEntity c WHERE c.frameworkVersionId = :frameworkVersionId)")
    void deleteByFrameworkVersionId(@Param("frameworkVersionId") UUID frameworkVersionId);
}
