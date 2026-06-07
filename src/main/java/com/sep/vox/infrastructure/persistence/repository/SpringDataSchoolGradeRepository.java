package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchoolGradeRepository extends JpaRepository<SchoolGradeJpaEntity, UUID> {
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);

    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT s
                FROM SchoolGradeJpaEntity s
                WHERE s.id = :schoolGradeId
                  AND s.schoolId = :schoolId
            """)
    Optional<SchoolGradeJpaEntity> findByIdAndSchoolIdForUpdate(
            @Param("schoolGradeId") UUID schoolGradeId,
            @Param("schoolId") UUID schoolId
    );

    Page<SchoolGradeJpaEntity> findAllBySchoolId(UUID schoolId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SchoolGradeJpaEntity s WHERE s.id = :id")
    Optional<SchoolGradeJpaEntity> findByIdForDelete(@Param("id") UUID id);

}
