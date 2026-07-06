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
    boolean existsByFrameworkVersionIdAndCodeAndIdNot(UUID frameworkVersionId, String code, UUID id);
    List<FrameworkCriterionJpaEntity> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkCriterionJpaEntity> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);

    @Modifying
    @Query("DELETE FROM FrameworkCriterionJpaEntity c WHERE c.frameworkVersionId = :frameworkVersionId")
    void deleteByFrameworkVersionId(@Param("frameworkVersionId") UUID frameworkVersionId);

    @Query("SELECT COUNT(c) > 0 FROM FrameworkCriterionJpaEntity c WHERE c.frameworkVersionId = :versionId AND c.code IN :codes")
    boolean existsByFrameworkVersionIdAndCodeIn(@Param("versionId") UUID versionId, @Param("codes") Collection<String> codes);   
}
