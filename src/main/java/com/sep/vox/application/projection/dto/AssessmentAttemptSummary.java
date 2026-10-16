package com.sep.vox.application.projection.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;

public record AssessmentAttemptSummary(
    UUID candidateId,
    UUID assessmentId,
    AssessmentCandidateStatus candidateStatus,
    UUID sessionId,
    Instant startedAt,
    Instant submittedAt,
    AssessmentSessionStatus status,
    boolean flagged,
    String flagReason,
    BigDecimal totalScore,
    /**
     * Thang điểm của rubric đã chấm lượt này -- thêm 2026-08-11.
     *
     * <p>Không có nó thì client phải đoán, và đã đoán sai: cả web lẫn Flutter tô màu điểm theo
     * ngưỡng cứng 80/45, đúng với thang 0-100 nhưng biến MỌI điểm của rubric thang 0-10 thành
     * màu đỏ, kể cả bài 10/10.
     *
     * <p>Danh sách lượt thi trộn nhiều kỳ thi, mà mỗi kỳ có thể dùng rubric khác thang -- nên
     * thang phải đi theo TỪNG lượt, không suy từ một cấu hình chung nào được.
     */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax,
    UUID rubricResultBandId,
    String rubricResultBandCode,
    String rubricResultBandName,
    AssessmentCandidateResultStatus resultStatus,
    /**
     * Lý do lượt thi bị xoá mềm, null nếu chưa xoá. Đi cùng {@code status = DELETED}.
     *
     * <p>Chỉ có giá trị ở đường đọc của quản trị trường / chủ tịch hội đồng
     * ({@code findByCandidateIdsIncludingDeleted}); đường của học sinh không trả về lượt đã xoá
     * nên trường này luôn null ở đó.
     */
    String deletedReason
) {
    // Auxiliary constructor for JPQL "SELECT NEW ...AssessmentAttemptSummary(...)"
    // projections: status/resultStatus are plain String columns on the JPA
    // entities (enum conversion normally happens at the domain-mapper layer,
    // not on the entity), so this overload accepts the raw strings straight
    // out of the query and converts them here, keeping the record itself
    // properly enum-typed for every other caller.
    public AssessmentAttemptSummary(
            UUID candidateId,
            UUID assessmentId,
            String candidateStatus,
            UUID sessionId,
            Instant startedAt,
            Instant submittedAt,
            String status,
            Boolean flagged,
            String flagReason,
            BigDecimal totalScore,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax,
            UUID rubricResultBandId,
            String rubricResultBandCode,
            String rubricResultBandName,
            String resultStatus,
            String deletedReason) {
        this(
            candidateId,
            assessmentId,
            candidateStatus == null ? null : AssessmentCandidateStatus.valueOf(candidateStatus),
            sessionId,
            startedAt,
            submittedAt,
            AssessmentSessionStatus.valueOf(status),
            Boolean.TRUE.equals(flagged),
            flagReason,
            totalScore,
            scoringScaleMin,
            scoringScaleMax,
            rubricResultBandId,
            rubricResultBandCode,
            rubricResultBandName,
            resultStatus == null ? null : AssessmentCandidateResultStatus.valueOf(resultStatus),
            deletedReason
        );
    }
}
