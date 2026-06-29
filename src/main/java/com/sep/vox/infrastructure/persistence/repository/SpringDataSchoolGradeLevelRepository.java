package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeLevelJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSchoolGradeLevelRepository extends JpaRepository<SchoolGradeLevelJpaEntity, UUID> {
    Optional<SchoolGradeLevelJpaEntity> findBySchoolId(UUID schoolId);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
    boolean existsBySchoolIdAndOrder(UUID schoolId, int order);
    List<SchoolGradeLevelJpaEntity> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);

    @Query("""
        SELECT sgl
        FROM SchoolGradeLevelJpaEntity sgl
        WHERE sgl.schoolId = :schoolId
            AND (:searchPattern IS NULL
                OR LOWER(sgl.code) LIKE :searchPattern
                OR LOWER(sgl.name) LIKE :searchPattern)
            AND (:status IS NULL OR sgl.status = :status)
        ORDER BY sgl.order ASC
        """)
    Page<SchoolGradeLevelJpaEntity> findBySchoolIdWithFilters(
        @Param("schoolId") UUID schoolId,
        @Param("searchPattern") String searchPattern,
        @Param("status") String status,
        Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE SchoolGradeLevelJpaEntity l SET
        l.name = COALESCE(:name, l.name),
        l.description = COALESCE(:description, l.description),
        l.order = COALESCE(:order, l.order),
        l.updatedAt = :updatedAt,
        l.updatedBy = :updatedBy
        WHERE l.id = :id
        """)
    int updateSchoolGradeLevelAtomic(
        @Param("id") UUID id,
        @Param("name") String name,
        @Param("description") String description,
        @Param("order") Integer order,
        @Param("updatedAt") OffsetDateTime updatedAt,
        @Param("updatedBy") UUID updatedBy
    );
}

