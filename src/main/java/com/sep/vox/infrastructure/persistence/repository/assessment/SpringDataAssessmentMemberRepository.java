package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentMemberJpaEntity;

public interface SpringDataAssessmentMemberRepository extends JpaRepository<AssessmentMemberJpaEntity, UUID> {
    List<AssessmentMemberJpaEntity> findByExamId(UUID examId);
    List<AssessmentMemberJpaEntity> findByExamIdIn(Collection<UUID> examIds);
    void deleteByExamId(UUID examId);
    Optional<AssessmentMemberJpaEntity> findByExamIdAndUserId(UUID examId, UUID userId);

    boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, String role);

    @Query("""
        SELECT CASE WHEN COUNT(em) > 0 THEN true ELSE false END
        FROM AssessmentMemberJpaEntity em
        JOIN AssessmentJpaEntity e ON e.id = em.examId
        WHERE em.userId = :userId
          AND em.role = :role
          AND e.schoolId = :schoolId
    """)
    boolean existsByUserIdAndRoleAndSchoolId(
        @Param("userId") UUID userId,
        @Param("role") String role,
        @Param("schoolId") UUID schoolId
    );

    @Query("""
        SELECT CASE WHEN COUNT(em) > 0 THEN true ELSE false END
        FROM AssessmentMemberJpaEntity em
        WHERE em.examId = :examId
          AND em.role = :role
          AND em.userId <> :excludeUserId
    """)
    boolean existsByExamIdAndRoleExcludingUserId(
        @Param("examId") UUID examId,
        @Param("role") String role,
        @Param("excludeUserId") UUID excludeUserId
    );

    @Query("""
        SELECT CASE WHEN COUNT(em) > 0 THEN true ELSE false END
        FROM AssessmentMemberJpaEntity em
        JOIN AssessmentJpaEntity e ON e.id = em.examId
        WHERE em.role = :role
          AND e.schoolId = :schoolId
          AND em.userId <> :excludeUserId
    """)
    boolean existsByRoleAndSchoolIdExcludingUserId(
        @Param("role") String role,
        @Param("schoolId") UUID schoolId,
        @Param("excludeUserId") UUID excludeUserId
    );

    @Query("""
        SELECT CASE WHEN (
            EXISTS (
                SELECT 1 FROM UserRoleJpaEntity ur
                JOIN RoleJpaEntity r ON r.id = ur.roleId
                WHERE ur.userId = :userId AND r.code = 'SCHOOL_ADMIN'
            )
            OR EXISTS (
                SELECT 1 FROM AssessmentMemberJpaEntity em
                WHERE em.examId = :examId AND em.userId = :userId AND em.role IN ('AUTHOR', 'CHAIR')
            )
        ) THEN true ELSE false END
        FROM AssessmentJpaEntity e
        WHERE e.id = :examId
    """)
    boolean canAttachBlueprint(@Param("examId") UUID examId, @Param("userId") UUID userId);

    @Query("""
        SELECT CASE WHEN (
            EXISTS (
                SELECT 1 FROM UserRoleJpaEntity ur
                JOIN RoleJpaEntity r ON r.id = ur.roleId
                WHERE ur.userId = :userId AND r.code = 'SCHOOL_ADMIN'
            )
            OR EXISTS (
                SELECT 1 FROM AssessmentMemberJpaEntity em
                WHERE em.examId = :examId AND em.userId = :userId AND em.role = 'CHAIR'
            )
        ) THEN true ELSE false END
        FROM AssessmentJpaEntity e
        WHERE e.id = :examId
    """)
    boolean canApproveBlueprintVersion(@Param("examId") UUID examId, @Param("userId") UUID userId);
}
