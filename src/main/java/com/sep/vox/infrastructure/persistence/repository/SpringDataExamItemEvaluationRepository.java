package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationJpaEntity;

public interface SpringDataExamItemEvaluationRepository extends JpaRepository<ExamItemEvaluationJpaEntity, UUID> {
    List<ExamItemEvaluationJpaEntity> findByResponseIdIn(Collection<UUID> responseIds);
    void deleteByResponseIdIn(Collection<UUID> responseIds);

    // Only AUTO_GRADED (AI) and FINALIZED (published re-grade) count as the
    // authoritative evaluation. UNDER_REVIEW is an appeal reviewer's in-progress
    // report and SUPERSEDED is an evaluation the appeal replaced; neither may
    // reach the score calculator or the candidate would see an unpublished
    // re-grade on their result screen.
    @Query("""
        SELECT e FROM ExamItemEvaluationJpaEntity e
        WHERE e.responseId = :responseId
        AND e.status IN ('AUTO_GRADED', 'FINALIZED')
        ORDER BY e.evaluatedAt DESC
        LIMIT 1
    """)
    Optional<ExamItemEvaluationJpaEntity> findLatestByResponseId(@Param("responseId") UUID responseId);

    // Latest evaluation per response, batched in one query instead of one
    // findLatestByResponseId call per response (N+1).
    // The status filter must be repeated inside the MAX subquery: without it the
    // subquery returns the timestamp of an UNDER_REVIEW row, which then matches
    // nothing in the outer query and the response drops out entirely.
    @Query("""
        SELECT e FROM ExamItemEvaluationJpaEntity e
        WHERE e.responseId IN :responseIds
        AND e.status IN ('AUTO_GRADED', 'FINALIZED')
        AND e.evaluatedAt = (
            SELECT MAX(e2.evaluatedAt) FROM ExamItemEvaluationJpaEntity e2
            WHERE e2.responseId = e.responseId
            AND e2.status IN ('AUTO_GRADED', 'FINALIZED')
        )
    """)
    List<ExamItemEvaluationJpaEntity> findLatestByResponseIdIn(@Param("responseIds") Collection<UUID> responseIds);

    // Bản AI của một response, KHÔNG lọc AUTO_GRADED/FINALIZED: sau khi giáo viên chấm lại,
    // bản AI mang SUPERSEDED, mà nó lại là nơi duy nhất có turn (audio/transcript/word
    // feedback) và bằng chứng AI (signals, validity, confidence). Bỏ bộ lọc đó đi thì màn
    // kết quả của học sinh mất nội dung câu hỏi ngay khi có người chấm lại.
    // UNDER_REVIEW loại tường minh: đó là báo cáo phúc khảo chưa công bố.
    // ORDER BY ... LIMIT 1 thay vì MAX(evaluatedAt) — hai hàng trùng mốc thời gian thì
    // phép so bằng trả về cả hai (cùng lý do đã ghi ở JpaExamGradingQueryRepository).
    @Query("""
        SELECT e FROM ExamItemEvaluationJpaEntity e
        WHERE e.responseId = :responseId
        AND e.engineType IN ('AI_SINGLE', 'AI_ENSEMBLE')
        AND e.status <> 'UNDER_REVIEW'
        AND e.evaluatedAt IS NOT NULL
        ORDER BY e.evaluatedAt DESC
        LIMIT 1
    """)
    Optional<ExamItemEvaluationJpaEntity> findLatestAiByResponseId(@Param("responseId") UUID responseId);
}
