package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.QuestionStatusCountInfo;
import com.sep.vox.application.query.repository.QuestionQueryRepository;
import com.sep.vox.domain.model.question.QuestionStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaQuestionQueryRepository implements QuestionQueryRepository {

    /**
     * The conditions below are a VERBATIM COPY of the WHERE part of
     * {@code SpringDataQuestionRepository.findAccessible}, dropping exactly two things: the
     * {@code :status} filter (because status is the dimension being grouped) and {@code ORDER BY} (meaningless
     * when aggregating). Changing one side and forgetting the other makes the number on the chart drift from the
     * list the user clicks into -- the kind of error nobody reports, it just silently gives a different total.
     *
     * <p>So never "optimize" this block on its own to make it shorter.
     */
    private static final String ACCESSIBLE_QUESTIONS_JPQL = """
        SELECT q.status, COUNT(q)
        FROM QuestionJpaEntity q
        JOIN QuestionTopicJpaEntity qt ON qt.id = q.questionTopicId
        JOIN QuestionBankJpaEntity qb ON qb.id = q.questionBankId
        WHERE (:questionBankId IS NULL OR q.questionBankId = :questionBankId)
          AND (:questionTopicId IS NULL OR q.questionTopicId = :questionTopicId)
          AND (:topicNamePattern IS NULL OR LOWER(qt.name) LIKE :topicNamePattern)
          AND (:type IS NULL OR q.type = :type)
          AND (:sharing IS NULL OR q.sharing = :sharing)
          AND (
                :scope IS NULL
                OR :scope = 'ALL'
                OR (:scope = 'MINE' AND q.createdBy = cast(:currentUserId as string))
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
                    FROM AssessmentPaperItemJpaEntity epi
                    JOIN AssessmentPaperJpaEntity ep ON ep.id = epi.paperId
                    JOIN AssessmentMemberJpaEntity em ON em.assessmentId = ep.assessmentId
                    WHERE epi.questionId = q.id
                      AND em.userId = :currentUserId
                )
                OR EXISTS (
                    SELECT 1
                    FROM AssessmentBlueprintSlotJpaEntity bs
                    JOIN AssessmentBlueprintVersionJpaEntity bv ON bv.id = bs.blueprintVersionId
                    JOIN AssessmentBlueprintJpaEntity b ON b.id = bv.blueprintId
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
                            q.createdBy = cast(:currentUserId as string)
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
        GROUP BY q.status
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<QuestionStatusCountInfo> countAccessibleByStatus(
            UUID currentUserId,
            UUID currentSchoolId,
            boolean systemAdmin,
            boolean schoolAdmin,
            UUID questionBankId,
            UUID questionTopicId,
            String topicNamePattern,
            String type,
            String sharing,
            String scope,
            String keywordPattern) {
        TypedQuery<Object[]> query = em.createQuery(ACCESSIBLE_QUESTIONS_JPQL, Object[].class);
        bindCaller(query, currentUserId, currentSchoolId, systemAdmin, schoolAdmin);
        bindFilters(query, questionBankId, questionTopicId, topicNamePattern, type, sharing, scope, keywordPattern);
        return query.getResultList().stream()
            .map(JpaQuestionQueryRepository::toStatusCount)
            .toList();
    }

    private static void bindCaller(
            Query query, UUID currentUserId, UUID currentSchoolId, boolean systemAdmin, boolean schoolAdmin) {
        query.setParameter("currentUserId", currentUserId)
            .setParameter("currentSchoolId", currentSchoolId)
            .setParameter("systemAdmin", systemAdmin)
            .setParameter("schoolAdmin", schoolAdmin);
    }

    private static void bindFilters(
            Query query, UUID questionBankId, UUID questionTopicId, String topicNamePattern, String type,
            String sharing, String scope, String keywordPattern) {
        query.setParameter("questionBankId", questionBankId)
            .setParameter("questionTopicId", questionTopicId)
            .setParameter("topicNamePattern", topicNamePattern)
            .setParameter("type", type)
            .setParameter("sharing", sharing)
            .setParameter("scope", scope)
            .setParameter("keywordPattern", keywordPattern);
    }

    /**
     * SELECT new ... cannot be used: the status column of QuestionJpaEntity is a String (see the
     * @Column on the entity, constrained by a CHECK rather than @Enumerated), so a constructor
     * taking QuestionStatus would not match. Converting String -> enum here is the right place: this is
     * the only place that knows how the entity stores it.
     */
    private static QuestionStatusCountInfo toStatusCount(Object[] row) {
        return new QuestionStatusCountInfo(
            QuestionStatus.valueOf((String) row[0]),
            ((Number) row[1]).longValue());
    }
}
