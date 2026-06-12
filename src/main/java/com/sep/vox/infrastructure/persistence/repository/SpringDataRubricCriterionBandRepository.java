package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricCriterionBandJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricCriterionBandRepository extends JpaRepository<RubricCriterionBandJpaEntity, UUID> {
    void deleteByCriterionId(UUID criterionId);


    @Modifying
    @Query("DELETE FROM RubricCriterionBandJpaEntity b WHERE b.criterionId IN (SELECT c.id FROM RubricCriterionJpaEntity c WHERE c.rubricVersionId = :rubricVersionId)")
    void deleteByRubricVersionId(@Param("rubricVersionId") UUID rubricVersionId);
}
