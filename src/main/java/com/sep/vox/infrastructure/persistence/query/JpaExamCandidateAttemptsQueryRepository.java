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

        // LEFT JOIN tới rubric_versions để mang THANG ĐIỂM theo từng lượt: lượt chưa có kết quả
        // (r null) hoặc kết quả không gắn rubric version thì thang là null, và client bỏ phần tô
        // màu thay vì tô sai. Không đặt chú thích này trong chuỗi JPQL -- HQL không nhận cú pháp
        // chú thích của SQL, và query dạng chuỗi chỉ vỡ lúc chạy chứ không vỡ lúc biên dịch.
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
                v.scoringScaleMin,
                v.scoringScaleMax,
                r.rubricResultBandId,
                b.code,
                b.name,
                r.status
            )
            FROM ExamSessionJpaEntity s
            JOIN ExamCandidateJpaEntity c ON c.id = s.candidateId
            LEFT JOIN ExamCandidateResultJpaEntity r ON r.sessionId = s.id
            LEFT JOIN RubricResultBandJpaEntity b ON b.id = r.rubricResultBandId
            LEFT JOIN RubricVersionJpaEntity v ON v.id = r.rubricVersionId
            WHERE s.candidateId IN :candidateIds
        """, ExamAttemptSummary.class)
            .setParameter("candidateIds", candidateIds)
            .getResultList();
    }
}
