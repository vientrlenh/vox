package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamSessionStatus;

public record ExamAttemptSummary(
    UUID candidateId,
    UUID examId,
    ExamCandidateStatus candidateStatus,
    UUID sessionId,
    Instant startedAt,
    Instant submittedAt,
    ExamSessionStatus status,
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
    ExamCandidateResultStatus resultStatus,
    /**
     * Lý do lượt thi bị xoá mềm, null nếu chưa xoá. Đi cùng {@code status = DELETED}.
     *
     * <p>Chỉ có giá trị ở đường đọc của quản trị trường / chủ tịch hội đồng
     * ({@code findByCandidateIdsIncludingDeleted}); đường của học sinh không trả về lượt đã xoá
     * nên trường này luôn null ở đó.
     */
    String deletedReason
) {
    // Auxiliary constructor for JPQL "SELECT NEW ...ExamAttemptSummary(...)"
    // projections: status/resultStatus are plain String columns on the JPA
    // entities (enum conversion normally happens at the domain-mapper layer,
    // not on the entity), so this overload accepts the raw strings straight
    // out of the query and converts them here, keeping the record itself
    // properly enum-typed for every other caller.
    public ExamAttemptSummary(
            UUID candidateId,
            UUID examId,
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
            examId,
            candidateStatus == null ? null : ExamCandidateStatus.valueOf(candidateStatus),
            sessionId,
            startedAt,
            submittedAt,
            ExamSessionStatus.valueOf(status),
            Boolean.TRUE.equals(flagged),
            flagReason,
            totalScore,
            scoringScaleMin,
            scoringScaleMax,
            rubricResultBandId,
            rubricResultBandCode,
            rubricResultBandName,
            resultStatus == null ? null : ExamCandidateResultStatus.valueOf(resultStatus),
            deletedReason
        );
    }
}
