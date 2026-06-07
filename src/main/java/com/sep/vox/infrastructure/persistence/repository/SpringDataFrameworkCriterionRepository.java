package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionJpaEntity;

public interface SpringDataFrameworkCriterionRepository extends JpaRepository<FrameworkCriterionJpaEntity, UUID> {
    List<FrameworkCriterionJpaEntity> findByFrameworkVersionId(UUID frameworkVersionId);

    @Modifying
    @Query("DELETE FROM FrameworkCriterionJpaEntity c WHERE c.frameworkVersionId = :frameworkVersionId")
    void deleteByFrameworkVersionId(@Param("frameworkVersionId") UUID frameworkVersionId);
}
