package com.sep.vox.domain.model.exam;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ma trận 4 vòng × 4 hành động — luật nghiệp vụ thuần, không phụ thuộc gì.
 *
 * <table>
 *   <caption>Vòng chấm × hành động</caption>
 *   <tr><th>Vòng</th><th>Bài đang ở</th><th>UPHELD</th><th>REGRADED</th>
 *       <th>INVALIDATED</th><th>CLEARED_INVALID</th></tr>
 *   <tr><td>INITIAL</td><td>PENDING_REVIEW</td><td>→ RELEASED</td><td>→ RELEASED</td>
 *       <td>→ INVALID</td><td>—</td></tr>
 *   <tr><td>SPOT_CHECK</td><td>RELEASED</td><td>giữ RELEASED</td><td>giữ RELEASED, đổi điểm</td>
 *       <td>→ INVALID</td><td>—</td></tr>
 *   <tr><td>REMEDIATION</td><td>INVALID</td><td>giữ INVALID</td><td>—</td>
 *       <td>—</td><td>→ PENDING_REVIEW</td></tr>
 *   <tr><td>APPEAL</td><td>APPEALED / RE_GRADING</td><td>→ RELEASED</td><td>→ RELEASED</td>
 *       <td>—</td><td>—</td></tr>
 * </table>
 *
 * <p>Bài RELEASED được hậu kiểm <em>vẫn giữ</em> RELEASED suốt quá trình: tiến độ nằm
 * ở dòng phân công chứ không ở {@link ExamCandidateResultStatus}. Nhờ vậy không phải
 * thêm giá trị nào vào enum đó — tránh cái bẫy {@code ddl-auto} không alter được
 * CHECK constraint.
 */
public final class GradingRoundPolicy {

    private GradingRoundPolicy() {}

    /** Trạng thái bài được phép nhận một phân công của vòng này. */
    public static Set<ExamCandidateResultStatus> assignableStatuses(GradingRoundType roundType) {
        return switch (roundType) {
            case INITIAL -> EnumSet.of(ExamCandidateResultStatus.PENDING_REVIEW);
            case SPOT_CHECK -> EnumSet.of(ExamCandidateResultStatus.RELEASED);
            case REMEDIATION -> EnumSet.of(ExamCandidateResultStatus.INVALID);
            // Đơn phúc khảo đưa bài sang APPEALED lúc tạo, rồi RE_GRADING khi đã giao
            // người chấm — chấp nhận cả hai để giao lại được sau khi đổi giáo viên.
            case APPEAL -> EnumSet.of(
                ExamCandidateResultStatus.APPEALED, ExamCandidateResultStatus.RE_GRADING);
        };
    }

    /**
     * Hợp của {@link #assignableStatuses} bốn vòng — "bài còn có thể nhận một vòng
     * chấm nào đó". Đây là mẫu số đúng cho con số <em>chưa phân công</em>: bài đã kết
     * thúc vòng đời ({@code FINAL}, {@code PASSED}, {@code FAILED},
     * {@code RETAKE_REQUIRED}…) không nằm trong tập này.
     *
     * <p>Sinh từ chính ma trận ở trên chứ không chép cứng, để thêm vòng mới là con số
     * tự đúng theo.
     */
    public static Set<ExamCandidateResultStatus> allAssignableStatuses() {
        var all = EnumSet.noneOf(ExamCandidateResultStatus.class);
        for (var roundType : GradingRoundType.values()) {
            all.addAll(assignableStatuses(roundType));
        }
        return all;
    }

    /** Hành động hợp lệ của vòng. {@code DECLINED} hợp lệ ở mọi vòng nên không nằm đây. */
    public static Set<GradingOutcome> allowedOutcomes(GradingRoundType roundType) {
        return switch (roundType) {
            case INITIAL, SPOT_CHECK -> EnumSet.of(
                GradingOutcome.UPHELD, GradingOutcome.REGRADED, GradingOutcome.INVALIDATED);
            case REMEDIATION -> EnumSet.of(GradingOutcome.UPHELD, GradingOutcome.CLEARED_INVALID);
            case APPEAL -> EnumSet.of(GradingOutcome.UPHELD, GradingOutcome.REGRADED);
        };
    }

    public static boolean isAssignable(GradingRoundType roundType, ExamCandidateResultStatus status) {
        return status != null && assignableStatuses(roundType).contains(status);
    }

    public static boolean isAllowed(GradingRoundType roundType, GradingOutcome outcome) {
        return outcome == GradingOutcome.DECLINED || allowedOutcomes(roundType).contains(outcome);
    }

    /**
     * Trạng thái bài sau hành động. {@code null} nghĩa là <em>giữ nguyên</em> — dùng
     * cho hậu kiểm (bài không nhấp nháy) và cho REMEDIATION/UPHELD (xác nhận vi phạm).
     *
     * @throws IllegalStateException khi hành động không hợp lệ với vòng; gọi
     *         {@link #isAllowed} trước để báo lỗi tiếng Việt đúng ngữ cảnh.
     */
    public static ExamCandidateResultStatus resultStatusAfter(
            GradingRoundType roundType, GradingOutcome outcome) {
        if (outcome == GradingOutcome.DECLINED) {
            return null;
        }
        if (!isAllowed(roundType, outcome)) {
            throw new IllegalStateException(
                "Hành động " + outcome + " không hợp lệ với vòng chấm " + roundType + ".");
        }
        return switch (roundType) {
            case INITIAL -> outcome == GradingOutcome.INVALIDATED
                ? ExamCandidateResultStatus.INVALID
                : ExamCandidateResultStatus.RELEASED;
            // Hậu kiểm: bài đã RELEASED thì giữ RELEASED (null = không đụng), trừ khi
            // kết luận vi phạm.
            case SPOT_CHECK -> outcome == GradingOutcome.INVALIDATED
                ? ExamCandidateResultStatus.INVALID
                : null;
            case REMEDIATION -> outcome == GradingOutcome.CLEARED_INVALID
                ? ExamCandidateResultStatus.PENDING_REVIEW
                : null;
            case APPEAL -> ExamCandidateResultStatus.RELEASED;
        };
    }

    /** Hành động bắt buộc phải kèm lý do — thiếu là không giải trình được về sau. */
    public static boolean requiresReason(GradingOutcome outcome) {
        return outcome == GradingOutcome.INVALIDATED
            || outcome == GradingOutcome.CLEARED_INVALID
            || outcome == GradingOutcome.DECLINED;
    }

    /** Hành động bắt buộc phải kèm điểm mới. */
    public static boolean requiresScores(GradingOutcome outcome) {
        return outcome == GradingOutcome.REGRADED;
    }
}
