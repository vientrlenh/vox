package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SpringDataFrameworkCriterionRepository extends JpaRepository<FrameworkCriterionJpaEntity, UUID> {

    boolean existsByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkCriterionJpaEntity> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkCriterionJpaEntity> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);

    @Modifying
    @Query("DELETE FROM FrameworkCriterionJpaEntity c WHERE c.frameworkVersionId = :frameworkVersionId")
    void deleteByFrameworkVersionId(@Param("frameworkVersionId") UUID frameworkVersionId);
}
