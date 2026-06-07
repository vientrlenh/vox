package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionBandJpaEntity;

public interface SpringDataFrameworkCriterionBandRepository extends JpaRepository<FrameworkCriterionBandJpaEntity, UUID> {
    List<FrameworkCriterionBandJpaEntity> findByFrameworkCriterionId(UUID frameworkCriterionId);
    List<FrameworkCriterionBandJpaEntity> findByFrameworkResultBandId(UUID frameworkResultBandId);

    @Modifying
    @Query("DELETE FROM FrameworkCriterionBandJpaEntity b WHERE b.frameworkCriterionId = :frameworkCriterionId")
    void deleteByFrameworkCriterionId(@Param("frameworkCriterionId") UUID frameworkCriterionId);
}
