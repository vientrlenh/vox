package com.sep.vox.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolGradeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchoolGradeRepository extends JpaRepository<SchoolGradeJpaEntity, UUID> {
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);

    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    // Thêm lock để đảm bảo không ai kịp sửa trạng thái khi bạn đang xóa
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SchoolGradeJpaEntity s WHERE s.id = :id AND s.schoolId = :schoolId")
    Optional<SchoolGradeJpaEntity> findByIdAndSchoolIdForDelete(
            @Param("id") UUID id,
            @Param("schoolId") UUID schoolId
    );

    Page<SchoolGradeJpaEntity> findAllBySchoolId(UUID schoolId, Pageable pageable);

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

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);

    @Modifying
    @Query("DELETE FROM SchoolGradeJpaEntity s WHERE s.id = :id AND s.schoolId = :schoolId")
    void deleteByIdAndSchoolId(@Param("id") UUID id, @Param("schoolId") UUID schoolId);

}
