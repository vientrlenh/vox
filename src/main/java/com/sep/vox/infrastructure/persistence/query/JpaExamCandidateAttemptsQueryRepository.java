package com.sep.vox.infrastructure.persistence.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ExamAttemptSummary;
import com.sep.vox.application.query.repository.ExamCandidateAttemptsQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaExamCandidateAttemptsQueryRepository implements ExamCandidateAttemptsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ExamAttemptSummary> findByCandidateIds(Collection<UUID> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }

        return em.createQuery("""
            SELECT NEW com.sep.vox.application.query.dto.ExamAttemptSummary(
                s.candidateId,
                s.examId,
                c.status,
                s.id,
                s.startedAt,
                s.submittedAt,
                s.status,
                s.flagged,
                s.flagReason,
                r.totalScore,
                r.rubricResultBandId,
                b.code,
                b.name,
                r.status
            )
            FROM ExamSessionJpaEntity s
            JOIN ExamCandidateJpaEntity c ON c.id = s.candidateId
            LEFT JOIN ExamCandidateResultJpaEntity r ON r.sessionId = s.id
            LEFT JOIN RubricResultBandJpaEntity b ON b.id = r.rubricResultBandId
            WHERE s.candidateId IN :candidateIds
        """, ExamAttemptSummary.class)
            .setParameter("candidateIds", candidateIds)
            .getResultList();
    }
}
