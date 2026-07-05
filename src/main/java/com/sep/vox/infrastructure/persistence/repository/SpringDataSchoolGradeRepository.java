package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;
import org.springframework.data.jpa.repository.Modifying;

public interface SpringDataSchoolGradeRepository extends JpaRepository<SchoolGradeJpaEntity, UUID>{
    @Query("""
        SELECT sg
        FROM SchoolGradeJpaEntity sg
        JOIN SchoolGradeLevelJpaEntity sgl 
            ON sgl.id = sg.schoolGradeLevelId
        WHERE sgl.schoolId = :schoolId 
            AND sg.code = :code
        """)
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(@Param("schoolId") UUID schoolId, @Param("code") String code);

    @Query("""
        SELECT sg
        FROM SchoolGradeJpaEntity sg
        JOIN SchoolGradeLevelJpaEntity sgl
            ON sgl.id = sg.schoolGradeLevelId
        WHERE sgl.schoolId = :schoolId
            AND sg.name = :name
        """)
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndName(@Param("schoolId") UUID schoolId, @Param("name") String name);

    @Query("""
        SELECT sg
        FROM SchoolGradeJpaEntity sg
        JOIN SchoolGradeLevelJpaEntity sgl 
            ON sgl.id = sg.schoolGradeLevelId
        WHERE sgl.schoolId = :schoolId 
            AND sg.code IN (:codes)
        """)
    List<SchoolGradeJpaEntity> findBySchoolIdAndCodeIn(@Param("schoolId") UUID schoolId, @Param("codes") Collection<String> codes);

    List<SchoolGradeJpaEntity> findByIdIn(Collection<UUID> ids);



    boolean existsBySchoolGradeLevelIdAndCode(UUID schoolGradeLevelId, String code);

    Optional<SchoolGradeJpaEntity> findBySchoolGradeLevelIdAndCode(UUID schoolGradeLevelId, String code);

    @Query("""
        SELECT g FROM SchoolGradeJpaEntity g
        JOIN SchoolGradeLevelJpaEntity l ON l.id = g.schoolGradeLevelId
        WHERE l.schoolId = :schoolId
            AND (:gradeLevelId IS NULL OR g.schoolGradeLevelId = :gradeLevelId)
        ORDER BY g.startDate DESC
        """)
    Page<SchoolGradeJpaEntity> findAllBySchoolId(
        @Param("schoolId") UUID schoolId,
        @Param("gradeLevelId") UUID gradeLevelId,
        Pageable pageable
    );


    @Modifying
    @Query("""

            UPDATE SchoolGradeJpaEntity g SET 
        g.name = COALESCE(:name, g.name),
        g.description = COALESCE(:description, g.description),
        g.startDate = COALESCE(:startDate, g.startDate),
        g.endDate = COALESCE(:endDate, g.endDate),
        g.updatedAt = :updatedAt,
        g.updatedBy = :updatedBy
        WHERE g.id = :id
        """)
    int updateSchoolGradeAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );


    @Query("SELECT COUNT(g) > 0 FROM SchoolGradeJpaEntity g WHERE g.schoolGradeLevelId IN (SELECT l.id FROM SchoolGradeLevelJpaEntity l WHERE l.schoolId = :schoolId) AND g.status = :status")
    boolean existsBySchoolIdAndStatus(@Param("schoolId") UUID schoolId, @Param("status") String status);

    boolean existsBySchoolGradeLevelId(UUID schoolGradeLevelId);

}