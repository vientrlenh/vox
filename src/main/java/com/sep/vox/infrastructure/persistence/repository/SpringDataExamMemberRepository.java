package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamMemberJpaEntity;

public interface SpringDataExamMemberRepository extends JpaRepository<ExamMemberJpaEntity, UUID> {
    List<ExamMemberJpaEntity> findByExamId(UUID examId);
    List<ExamMemberJpaEntity> findByExamIdIn(Collection<UUID> examIds);
    Optional<ExamMemberJpaEntity> findByExamIdAndUserId(UUID examId, UUID userId);

    boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, String role);

    @Query("""
        SELECT CASE WHEN COUNT(em) > 0 THEN true ELSE false END
        FROM ExamMemberJpaEntity em
        JOIN ExamJpaEntity e ON e.id = em.examId
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
        FROM ExamMemberJpaEntity em
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
        FROM ExamMemberJpaEntity em
        JOIN ExamJpaEntity e ON e.id = em.examId
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
                SELECT 1 FROM ExamMemberJpaEntity em
                WHERE em.examId = :examId AND em.userId = :userId AND em.role = 'AUTHOR'
            )
        ) THEN true ELSE false END
        FROM ExamJpaEntity e
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
                SELECT 1 FROM ExamMemberJpaEntity em
                WHERE em.examId = :examId AND em.userId = :userId AND em.role = 'CHAIR'
            )
        ) THEN true ELSE false END
        FROM ExamJpaEntity e
        WHERE e.id = :examId
    """)
    boolean canApproveBlueprintVersion(@Param("examId") UUID examId, @Param("userId") UUID userId);
}
