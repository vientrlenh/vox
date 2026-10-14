package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkVersionJpaEntity;

public interface SpringDataFrameworkVersionRepository extends JpaRepository<FrameworkVersionJpaEntity, UUID> {
    boolean existsByFrameworkIdAndStatus(UUID frameworkId, String status);

    @Query("""
        SELECT COUNT(fv) FROM FrameworkVersion fv 
        WHERE fv.frameworkId = :frameworkId 
            AND fv.status <> 'DRAFT'
    """)
    long countPublishedAndArchivedOnFrameworkId(@Param("frameworkId") UUID frameworkId);
}
