package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.gradelevel.GradeLevelBandScopeJpaEntity;

public interface SpringDataGradeLevelBandScopeRepository extends JpaRepository<GradeLevelBandScopeJpaEntity, UUID> {

    Optional<GradeLevelBandScopeJpaEntity> findByGradeLevelIdAndFrameworkVersionId(
            UUID gradeLevelId, UUID frameworkVersionId);

    List<GradeLevelBandScopeJpaEntity> findByGradeLevelIdInAndFrameworkVersionIdIn(
            Collection<UUID> gradeLevelIds, Collection<UUID> frameworkVersionIds);

    // A band can be the ceiling of one grade and the default of another -- both columns must be checked.
    @Query("""
            SELECT COUNT(s) > 0 FROM GradeLevelBandScopeJpaEntity s
            WHERE s.defaultTargetBandId = :bandId OR s.hardMaxBandId = :bandId
            """)
    boolean existsByBandId(@Param("bandId") UUID bandId);
}
