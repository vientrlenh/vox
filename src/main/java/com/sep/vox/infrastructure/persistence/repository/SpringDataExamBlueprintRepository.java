package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintJpaEntity;

public interface SpringDataExamBlueprintRepository extends JpaRepository<ExamBlueprintJpaEntity, UUID> {

    @Query("""
        SELECT b
        FROM ExamBlueprintJpaEntity b
        WHERE (:schoolId IS NULL OR b.schoolId = :schoolId)
          AND (:isActive IS NULL OR b.isActive = :isActive)
          AND (:languageId IS NULL OR b.languageId = :languageId)
          AND (
                :examKind IS NULL
                OR EXISTS (
                    SELECT 1 FROM ExamJpaEntity e
                    WHERE e.blueprintId = b.id AND e.kind = :examKind
                )
              )
          AND (
                :keywordPattern IS NULL
                OR LOWER(b.code) LIKE :keywordPattern
                OR LOWER(b.name) LIKE :keywordPattern
              )
          AND (
                :systemAdmin = true
                OR b.schoolId = :currentSchoolId
              )
        ORDER BY b.updatedAt DESC
    """)
    Page<ExamBlueprintJpaEntity> findAccessible(
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("schoolId") UUID schoolId,
        @Param("isActive") Boolean isActive,
        @Param("languageId") UUID languageId,
        @Param("examKind") String examKind,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM ExamJpaEntity e
        WHERE e.blueprintId = :blueprintId
    """)
    boolean existsUsedByExam(@Param("blueprintId") UUID blueprintId);

    @Query("""
        SELECT CASE WHEN (
            b.schoolId = :schoolId
            AND (
                EXISTS (
                    SELECT 1 FROM UserRoleJpaEntity ur
                    JOIN RoleJpaEntity r ON r.id = ur.roleId
                    WHERE ur.userId = :userId AND r.code = 'SCHOOL_ADMIN'
                )
                OR EXISTS (
                    SELECT 1 FROM ExamJpaEntity e
                    JOIN ExamMemberJpaEntity em ON em.examId = e.id
                    WHERE e.blueprintId = b.id AND em.userId = :userId AND em.role = 'AUTHOR'
                )
                OR (
                    NOT EXISTS (SELECT 1 FROM ExamJpaEntity e2 WHERE e2.blueprintId = b.id)
                    AND b.createdBy = :userId
                )
            )
        ) THEN true ELSE false END
        FROM ExamBlueprintJpaEntity b
        WHERE b.id = :blueprintId
    """)
    boolean canEditBlueprint(
        @Param("blueprintId") UUID blueprintId,
        @Param("userId") UUID userId,
        @Param("schoolId") UUID schoolId
    );

    @Query("""
        SELECT CASE WHEN (
            b.schoolId = :schoolId
            AND (
                (
                    NOT EXISTS (SELECT 1 FROM ExamJpaEntity e0 WHERE e0.blueprintId = b.id)
                    AND EXISTS (
                        SELECT 1 FROM UserRoleJpaEntity ur
                        JOIN RoleJpaEntity r ON r.id = ur.roleId
                        WHERE ur.userId = :userId AND r.code = 'SCHOOL_ADMIN'
                    )
                )
                OR EXISTS (
                    SELECT 1 FROM ExamJpaEntity e
                    JOIN ExamMemberJpaEntity em ON em.examId = e.id
                    WHERE e.blueprintId = b.id AND em.userId = :userId AND em.role IN ('CHAIR', 'REVIEWER')
                )
            )
        ) THEN true ELSE false END
        FROM ExamBlueprintJpaEntity b
        WHERE b.id = :blueprintId
    """)
    boolean canChangeVersionStatus(
        @Param("blueprintId") UUID blueprintId,
        @Param("userId") UUID userId,
        @Param("schoolId") UUID schoolId
    );

    @Query("""
        SELECT CASE WHEN (
            b.schoolId = :schoolId
        ) THEN true ELSE false END
        FROM ExamBlueprintJpaEntity b
        WHERE b.id = :blueprintId
    """)
    boolean canViewBlueprint(
        @Param("blueprintId") UUID blueprintId,
        @Param("userId") UUID userId,
        @Param("schoolId") UUID schoolId
    );
}
