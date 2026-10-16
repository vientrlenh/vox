package com.sep.vox.infrastructure.persistence.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.projection.dto.AssessmentAttemptSummary;
import com.sep.vox.application.projection.repository.AssessmentCandidateAttemptsQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaAssessmentCandidateAttemptsQueryRepository implements AssessmentCandidateAttemptsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // LEFT JOIN tới rubric_versions để mang THANG ĐIỂM theo từng lượt: lượt chưa có kết quả
    // (r null) hoặc kết quả không gắn rubric version thì thang là null, và client bỏ phần tô
    // màu thay vì tô sai. Không đặt chú thích này trong chuỗi JPQL -- HQL không nhận cú pháp
    // chú thích của SQL, và query dạng chuỗi chỉ vỡ lúc chạy chứ không vỡ lúc biên dịch.
    private static final String SELECT_ATTEMPTS = """
        SELECT NEW com.sep.vox.application.query.dto.AssessmentAttemptSummary(
            s.candidateId,
            s.assessmentId,
            c.status,
            s.id,
            s.startedAt,
            s.submittedAt,
            s.status,
            s.flagged,
            s.flagReason,
            r.totalScore,
            v.scoringScaleMin,
            v.scoringScaleMax,
            r.rubricResultBandId,
            b.code,
            b.name,
            r.status,
            s.deletedReason
        )
        FROM AssessmentSessionJpaEntity s
        JOIN AssessmentCandidateJpaEntity c ON c.id = s.candidateId
        LEFT JOIN AssessmentCandidateResultJpaEntity r ON r.sessionId = s.id
        LEFT JOIN RubricResultBandJpaEntity b ON b.id = r.rubricResultBandId
        LEFT JOIN RubricVersionJpaEntity v ON v.id = r.rubricVersionId
        WHERE s.candidateId IN :candidateIds
        """;

    @Override
    public List<AssessmentAttemptSummary> findByCandidateIds(Collection<UUID> candidateIds) {
        return query(candidateIds, SELECT_ATTEMPTS + " AND s.status <> 'DELETED'");
    }

    @Override
    public List<AssessmentAttemptSummary> findByCandidateIdsIncludingDeleted(Collection<UUID> candidateIds) {
        return query(candidateIds, SELECT_ATTEMPTS);
    }

    private List<AssessmentAttemptSummary> query(Collection<UUID> candidateIds, String jpql) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery(jpql, AssessmentAttemptSummary.class)
            .setParameter("candidateIds", candidateIds)
            .getResultList();
    }
}
