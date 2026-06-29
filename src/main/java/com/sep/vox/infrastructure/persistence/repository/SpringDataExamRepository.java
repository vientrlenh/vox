package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;

public interface SpringDataExamRepository extends JpaRepository<ExamJpaEntity, UUID> {

    @Query("""
        SELECT e
        FROM ExamJpaEntity e
        WHERE (:schoolId IS NULL OR e.schoolId = :schoolId)
          AND (
                :schoolClassId IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM ExamCandidateJpaEntity ec
                    JOIN SchoolClassUserJpaEntity scu ON scu.userId = ec.studentId
                    WHERE ec.examId = e.id
                      AND scu.schoolClassId = :schoolClassId
                      AND scu.isActive = true
                )
              )
          AND (:kind IS NULL OR e.kind = :kind)
          AND (:status IS NULL OR e.status = :status)
          AND (
                :keyword IS NULL
                OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
          AND (
                :systemAdmin = true
                OR (:schoolAdmin = true AND e.schoolId = :currentSchoolId)
                OR EXISTS (
                    SELECT 1
                    FROM ExamMemberJpaEntity em
                    WHERE em.examId = e.id
                      AND em.userId = :currentUserId
                )
              )
        ORDER BY e.updatedAt DESC
    """)
    Page<ExamJpaEntity> findAccessible(
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("schoolId") UUID schoolId,
        @Param("schoolClassId") UUID schoolClassId,
        @Param("kind") String kind,
        @Param("status") String status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    boolean existsByBlueprintId(UUID blueprintId);

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM ExamSessionJpaEntity s
        WHERE s.examId = :examId
          AND s.submittedAt IS NOT NULL
    """)
    boolean existsSubmittedSessionByExamId(@Param("examId") UUID examId);
}
