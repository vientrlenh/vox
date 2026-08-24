package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;
import org.springframework.data.jpa.repository.Modifying;

// school_grades giờ mang school_id trực tiếp, nên các truy vấn theo trường không còn phải JOIN
// sang bảng khối lớp để suy ra schoolId như bản cũ -- phần lớn rút về derived query.
public interface SpringDataSchoolGradeRepository extends JpaRepository<SchoolGradeJpaEntity, UUID>{

    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);

    Optional<SchoolGradeJpaEntity> findBySchoolIdAndName(UUID schoolId, String name);

    // Nhận codes đã được Impl uppercase sẵn; entity-side vẫn bọc UPPER() để phòng dữ liệu cũ lỡ không đồng nhất case.
    @Query("""
        SELECT sg
        FROM SchoolGradeJpaEntity sg
        WHERE sg.schoolId = :schoolId
            AND UPPER(sg.code) IN (:codes)
        """)
    List<SchoolGradeJpaEntity> findBySchoolIdAndCodeIn(@Param("schoolId") UUID schoolId, @Param("codes") Collection<String> codes);

    List<SchoolGradeJpaEntity> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names);

    List<SchoolGradeJpaEntity> findByIdIn(Collection<UUID> ids);

    boolean existsBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code);

    Optional<SchoolGradeJpaEntity> findBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code);

    @Query("""
        SELECT g FROM SchoolGradeJpaEntity g
        WHERE g.schoolId = :schoolId
            AND (:gradeLevelId IS NULL OR g.gradeLevelId = :gradeLevelId)
            AND (:status IS NULL OR g.status = :status)
            AND (:status IS NOT NULL OR g.status <> 'ARCHIVED')
        ORDER BY g.startDate DESC
        """)
    Page<SchoolGradeJpaEntity> findAllBySchoolId(
        @Param("schoolId") UUID schoolId,
        @Param("gradeLevelId") UUID gradeLevelId,
        @Param("status") String status,
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
            @Param("updatedAt") Instant updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);

    boolean existsByGradeLevelId(UUID gradeLevelId);

    boolean existsByGradeLevelIdAndStatusNot(UUID gradeLevelId, String status);

}
