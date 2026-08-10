package com.sep.vox.infrastructure.persistence.query;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.QuestionBankStatsDto;
import com.sep.vox.application.query.repository.QuestionBankStatsQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaQuestionBankStatsQueryRepository implements QuestionBankStatsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public QuestionBankStatsDto countForSchool(UUID schoolId) {
        var totalQuestionBanks = em.createQuery("""
            SELECT COUNT(qb)
            FROM QuestionBankJpaEntity qb
            WHERE qb.schoolId = :schoolId
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .getSingleResult();

        var counts = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.QuestionBankStatsDto(
                COUNT(q),
                0L,
                COALESCE(SUM(CASE WHEN q.status = 'DRAFT' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.status = 'SUBMITTED_FOR_REVIEW' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.status = 'REVISION_REQUESTED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.status = 'APPROVED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.status = 'REJECTED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.status = 'PUBLISHED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.status = 'ARCHIVED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.type = 'READ_ALOUD' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.type = 'SHORT_ANSWER' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.type = 'LONG_ANSWER' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.type = 'OPINION' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN q.type = 'DESCRIPTION' THEN 1L ELSE 0L END), 0L)
            )
            FROM QuestionJpaEntity q
            JOIN QuestionBankJpaEntity qb ON qb.id = q.questionBankId
            WHERE qb.schoolId = :schoolId
        """, QuestionBankStatsDto.class)
            .setParameter("schoolId", schoolId)
            .getSingleResult();

        return new QuestionBankStatsDto(
            counts.totalQuestions(),
            totalQuestionBanks,
            counts.draft(),
            counts.submittedForReview(),
            counts.revisionRequested(),
            counts.approved(),
            counts.rejected(),
            counts.published(),
            counts.archived(),
            counts.readAloud(),
            counts.shortAnswer(),
            counts.longAnswer(),
            counts.opinion(),
            counts.description()
        );
    }
}
