package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;
import org.springframework.data.jpa.repository.Modifying;

public interface SpringDataSchoolGradeRepository extends JpaRepository<SchoolGradeJpaEntity, UUID>{
    @Query("""
        select grade
        from SchoolGradeJpaEntity grade
        join SchoolGradeLevelJpaEntity level on level.id = grade.schoolGradeLevelId
            where level.schoolId = :schoolId and grade.code = :code
        """)
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(@Param("schoolId") UUID schoolId, @Param("code") String code);

    @Query("""

            select grade
            from SchoolGradeJpaEntity grade
            join SchoolGradeLevelJpaEntity level on level.id = grade.schoolGradeLevelId
            where level.schoolId = :schoolId and grade.code in :codes
        """)
    List<SchoolGradeJpaEntity> findBySchoolIdAndCodeIn(@Param("schoolId") UUID schoolId, @Param("codes") Collection<String> codes);




    boolean existsBySchoolGradeLevelIdAndCode(UUID schoolGradeLevelId, String code);

    @Query("SELECT g FROM SchoolGradeJpaEntity g WHERE g.schoolGradeLevelId IN (SELECT l.id FROM SchoolGradeLevelJpaEntity l WHERE l.schoolId = :schoolId)")
    Page<SchoolGradeJpaEntity> findAllBySchoolId(@Param("schoolId") UUID schoolId, Pageable pageable);


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