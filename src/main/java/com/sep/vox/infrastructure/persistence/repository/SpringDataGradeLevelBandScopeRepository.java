package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.GradeLevelBandScopeJpaEntity;

public interface SpringDataGradeLevelBandScopeRepository extends JpaRepository<GradeLevelBandScopeJpaEntity, UUID> {

    Optional<GradeLevelBandScopeJpaEntity> findByGradeLevelIdAndFrameworkVersionId(
            UUID gradeLevelId, UUID frameworkVersionId);

    List<GradeLevelBandScopeJpaEntity> findByGradeLevelIdInAndFrameworkVersionIdIn(
            Collection<UUID> gradeLevelIds, Collection<UUID> frameworkVersionIds);

    // Một bậc có thể vừa là trần của khối này vừa là mặc định của khối kia -- phải soi cả hai cột.
    @Query("""
            SELECT COUNT(s) > 0 FROM GradeLevelBandScopeJpaEntity s
            WHERE s.defaultTargetBandId = :bandId OR s.hardMaxBandId = :bandId
            """)
    boolean existsByBandId(@Param("bandId") UUID bandId);
}
