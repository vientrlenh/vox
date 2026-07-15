package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;

public interface SpringDataQuestionRepository extends JpaRepository<QuestionJpaEntity, UUID> {
    List<QuestionJpaEntity> findBySecurePoolId(UUID securePoolId);
    List<QuestionJpaEntity> findByQuestionBankId(UUID questionBankId);
    List<QuestionJpaEntity> findByQuestionTopicId(UUID questionTopicId);
    boolean existsByQuestionBankIdAndCode(UUID questionBankId, String code);
    boolean existsByQuestionBankIdAndQuestionTopicIdAndQuestionText(
        UUID questionBankId,
        UUID questionTopicId,
        String questionText
    );

    @Query("""
        SELECT q
        FROM QuestionJpaEntity q
        JOIN QuestionTopicJpaEntity qt ON qt.id = q.questionTopicId
        JOIN QuestionBankJpaEntity qb ON qb.id = q.questionBankId
        WHERE (:questionBankId IS NULL OR q.questionBankId = :questionBankId)
          AND (:questionTopicId IS NULL OR q.questionTopicId = :questionTopicId)
          AND (:topicNamePattern IS NULL OR LOWER(qt.name) LIKE :topicNamePattern)
          AND (:status IS NULL OR q.status = :status)
          AND (:type IS NULL OR q.type = :type)
          AND (:sharing IS NULL OR q.sharing = :sharing)
          AND (
                :scope IS NULL
                OR :scope = 'ALL'
                OR (:scope = 'MINE' AND q.createdBy = :currentUserId)
                OR (
                    :scope = 'COLLABORATING'
                    AND EXISTS (
                        SELECT 1
                        FROM QuestionCollaboratorJpaEntity qc
                        WHERE qc.questionId = q.id
                          AND qc.userId = :currentUserId
                    )
                )
                OR (
                    :scope = 'REVIEWING'
                    AND EXISTS (
                        SELECT 1
                        FROM QuestionCollaboratorJpaEntity qc
                        WHERE qc.questionId = q.id
                          AND qc.userId = :currentUserId
                          AND qc.permission = 'CAN_EDIT'
                    )
                )
              )
          AND (
                :keywordPattern IS NULL
                OR LOWER(q.code) LIKE :keywordPattern
                OR LOWER(q.questionText) LIKE :keywordPattern
              )
          AND (
                (:systemAdmin = true AND qb.ownerType = 'SYSTEM')
                OR EXISTS (
                    SELECT 1
                    FROM ExamPaperItemJpaEntity epi
                    JOIN ExamPaperJpaEntity ep ON ep.id = epi.paperId
                    JOIN ExamMemberJpaEntity em ON em.examId = ep.examId
                    WHERE epi.questionId = q.id
                      AND em.userId = :currentUserId
                )
                OR EXISTS (
                    SELECT 1
                    FROM ExamBlueprintSlotJpaEntity bs
                    JOIN ExamBlueprintVersionJpaEntity bv ON bv.id = bs.blueprintVersionId
                    JOIN ExamBlueprintJpaEntity b ON b.id = bv.blueprintId
                    WHERE bs.fixedQuestionId = q.id
                      AND (:systemAdmin = true OR b.schoolId = :currentSchoolId)
                )
                OR (
                    (
                        :systemAdmin = true
                        AND qb.ownerType = 'SCHOOL'
                        AND qb.status = 'PUBLISHED'
                        AND qt.status = 'PUBLISHED'
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        qb.ownerType = 'SYSTEM'
                        AND qb.status = 'PUBLISHED'
                        AND qt.status = 'PUBLISHED'
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        :schoolAdmin = true
                        AND (
                            qb.schoolId = :currentSchoolId
                            OR (
                                qb.status = 'PUBLISHED'
                                AND qt.status = 'PUBLISHED'
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                    OR (
                        :schoolAdmin = false
                        AND (
                            q.createdBy = :currentUserId
                            OR EXISTS (
                                SELECT 1
                                FROM QuestionCollaboratorJpaEntity qc
                                WHERE qc.questionId = q.id
                                  AND qc.userId = :currentUserId
                            )
                            OR (
                                qb.schoolId = :currentSchoolId
                                AND qb.ownerType = 'SCHOOL'
                                AND qb.status = 'PUBLISHED'
                                AND qt.status = 'PUBLISHED'
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                )
              )
        ORDER BY q.createdAt DESC, q.updatedAt DESC
    """)
    Page<QuestionJpaEntity> findAccessible(
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("questionBankId") UUID questionBankId,
        @Param("questionTopicId") UUID questionTopicId,
        @Param("topicNamePattern") String topicNamePattern,
        @Param("status") String status,
        @Param("type") String type,
        @Param("sharing") String sharing,
        @Param("scope") String scope,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    @Query("""
        SELECT q
        FROM QuestionJpaEntity q
        JOIN QuestionTopicJpaEntity qt ON qt.id = q.questionTopicId
        JOIN QuestionBankJpaEntity qb ON qb.id = q.questionBankId
        WHERE (:questionBankId IS NULL OR q.questionBankId = :questionBankId)
          AND (:questionTopicId IS NULL OR q.questionTopicId = :questionTopicId)
          AND (:topicNamePattern IS NULL OR LOWER(qt.name) LIKE :topicNamePattern)
          AND (:status IS NULL OR q.status = :status)
          AND (:type IS NULL OR q.type = :type)
          AND (:sharing IS NULL OR q.sharing = :sharing)
          AND (
                :scope IS NULL
                OR :scope = 'ALL'
                OR (:scope = 'MINE' AND q.createdBy = :currentUserId)
                OR (
                    :scope = 'COLLABORATING'
                    AND EXISTS (
                        SELECT 1
                        FROM QuestionCollaboratorJpaEntity qc
                        WHERE qc.questionId = q.id
                          AND qc.userId = :currentUserId
                          AND qc.permission IN ('CAN_USE', 'CAN_EDIT')
                    )
                )
              )
          AND (
                :keywordPattern IS NULL
                OR LOWER(q.code) LIKE :keywordPattern
                OR LOWER(q.questionText) LIKE :keywordPattern
              )
          AND (
                (:systemAdmin = true AND qb.ownerType = 'SYSTEM')
                OR EXISTS (
                    SELECT 1
                    FROM ExamPaperItemJpaEntity epi
                    JOIN ExamPaperJpaEntity ep ON ep.id = epi.paperId
                    JOIN ExamMemberJpaEntity em ON em.examId = ep.examId
                    WHERE epi.questionId = q.id
                      AND em.userId = :currentUserId
                )
                OR EXISTS (
                    SELECT 1
                    FROM ExamBlueprintSlotJpaEntity bs
                    JOIN ExamBlueprintVersionJpaEntity bv ON bv.id = bs.blueprintVersionId
                    JOIN ExamBlueprintJpaEntity b ON b.id = bv.blueprintId
                    WHERE bs.fixedQuestionId = q.id
                      AND (:systemAdmin = true OR b.schoolId = :currentSchoolId)
                )
                OR (
                    (
                        :systemAdmin = true
                        AND qb.ownerType = 'SCHOOL'
                        AND qb.status = 'PUBLISHED'
                        AND qt.status = 'PUBLISHED'
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        qb.ownerType = 'SYSTEM'
                        AND qb.status = 'PUBLISHED'
                        AND qt.status = 'PUBLISHED'
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        :schoolAdmin = true
                        AND (
                            qb.schoolId = :currentSchoolId
                            OR (
                                qb.status = 'PUBLISHED'
                                AND qt.status = 'PUBLISHED'
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                    OR (
                        :schoolAdmin = false
                        AND (
                            q.createdBy = :currentUserId
                            OR EXISTS (
                                SELECT 1
                                FROM QuestionCollaboratorJpaEntity qc
                                WHERE qc.questionId = q.id
                                  AND qc.userId = :currentUserId
                                  AND qc.permission IN ('CAN_USE', 'CAN_EDIT')
                            )
                            OR (
                                qb.schoolId = :currentSchoolId
                                AND qb.ownerType = 'SCHOOL'
                                AND qb.status = 'PUBLISHED'
                                AND qt.status = 'PUBLISHED'
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                )
              )
        ORDER BY q.createdAt DESC, q.updatedAt DESC
    """)
    Page<QuestionJpaEntity> findAccessibleForExamPaper(
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("questionBankId") UUID questionBankId,
        @Param("questionTopicId") UUID questionTopicId,
        @Param("topicNamePattern") String topicNamePattern,
        @Param("status") String status,
        @Param("type") String type,
        @Param("sharing") String sharing,
        @Param("scope") String scope,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    @Query("""
        SELECT q
        FROM QuestionJpaEntity q
        LEFT JOIN QuestionTopicJpaEntity qt ON qt.id = q.questionTopicId
        JOIN QuestionBankJpaEntity qb ON qb.id = q.questionBankId
        WHERE q.id = :id
          AND (
                (:systemAdmin = true AND qb.ownerType = 'SYSTEM')
                OR EXISTS (
                    SELECT 1
                    FROM ExamPaperItemJpaEntity epi
                    JOIN ExamPaperJpaEntity ep ON ep.id = epi.paperId
                    JOIN ExamMemberJpaEntity em ON em.examId = ep.examId
                    WHERE epi.questionId = q.id
                      AND em.userId = :currentUserId
                )
                OR EXISTS (
                    SELECT 1
                    FROM ExamBlueprintSlotJpaEntity bs
                    JOIN ExamBlueprintVersionJpaEntity bv ON bv.id = bs.blueprintVersionId
                    JOIN ExamBlueprintJpaEntity b ON b.id = bv.blueprintId
                    WHERE bs.fixedQuestionId = q.id
                      AND (:systemAdmin = true OR b.schoolId = :currentSchoolId)
                )
                OR (
                    (
                        :systemAdmin = true
                        AND qb.ownerType = 'SCHOOL'
                        AND qb.status = 'PUBLISHED'
                        AND (qt IS NULL OR qt.status = 'PUBLISHED')
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        qb.ownerType = 'SYSTEM'
                        AND qb.status = 'PUBLISHED'
                        AND (qt IS NULL OR qt.status = 'PUBLISHED')
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        :schoolAdmin = true
                        AND (
                            qb.schoolId = :currentSchoolId
                            OR (
                                qb.status = 'PUBLISHED'
                                AND (qt IS NULL OR qt.status = 'PUBLISHED')
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                    OR (
                        :schoolAdmin = false
                        AND (
                            q.createdBy = :currentUserId
                            OR EXISTS (
                                SELECT 1
                                FROM QuestionCollaboratorJpaEntity qc
                                WHERE qc.questionId = q.id
                                  AND qc.userId = :currentUserId
                            )
                            OR (
                                qb.schoolId = :currentSchoolId
                                AND qb.ownerType = 'SCHOOL'
                                AND qb.status = 'PUBLISHED'
                                AND (qt IS NULL OR qt.status = 'PUBLISHED')
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                )
              )
    """)
    Optional<QuestionJpaEntity> findAccessibleById(
        @Param("id") UUID id,
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin
    );

    @Query("""
        SELECT CASE WHEN COUNT(epi) > 0 THEN true ELSE false END
        FROM ExamPaperItemJpaEntity epi
        WHERE epi.questionId = :questionId
    """)
    boolean existsUsedInExam(@Param("questionId") UUID questionId);

    @Query("""
        SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END
        FROM QuestionJpaEntity q
        WHERE q.questionBankId = :questionBankId
          AND q.status = 'PUBLISHED'
          AND EXISTS (SELECT 1 FROM ExamPaperItemJpaEntity epi WHERE epi.questionId = q.id)
    """)
    boolean existsPublishedAndUsedByQuestionBankId(@Param("questionBankId") UUID questionBankId);

    @Query("""
        SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END
        FROM QuestionJpaEntity q
        WHERE q.questionTopicId = :questionTopicId
          AND q.status = 'PUBLISHED'
          AND EXISTS (SELECT 1 FROM ExamPaperItemJpaEntity epi WHERE epi.questionId = q.id)
    """)
    boolean existsPublishedAndUsedByQuestionTopicId(@Param("questionTopicId") UUID questionTopicId);
}
