package com.sep.vox.domain.service.assessment;

import java.util.EnumSet;
import java.util.Set;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingOutcome;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;

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
 * ở dòng phân công chứ không ở {@link AssessmentCandidateResultStatus}. Nhờ vậy không phải
 * thêm giá trị nào vào enum đó — tránh cái bẫy {@code ddl-auto} không alter được
 * CHECK constraint.
 */
public final class GradingRoundPolicy {

    private GradingRoundPolicy() {}

    /** Trạng thái bài được phép nhận một phân công của vòng này. */
    public static Set<AssessmentCandidateResultStatus> assignableStatuses(AssessmentGradingRoundType roundType) {
        return switch (roundType) {
            case INITIAL -> EnumSet.of(AssessmentCandidateResultStatus.PENDING_REVIEW);
            case SPOT_CHECK -> EnumSet.of(AssessmentCandidateResultStatus.RELEASED);
            case REMEDIATION -> EnumSet.of(AssessmentCandidateResultStatus.INVALID);
            // Đơn phúc khảo đưa bài sang APPEALED lúc tạo, rồi RE_GRADING khi đã giao
            // người chấm — chấp nhận cả hai để giao lại được sau khi đổi giáo viên.
            case APPEAL -> EnumSet.of(
                AssessmentCandidateResultStatus.APPEALED, AssessmentCandidateResultStatus.RE_GRADING);
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
    public static Set<AssessmentCandidateResultStatus> allAssignableStatuses() {
        var all = EnumSet.noneOf(AssessmentCandidateResultStatus.class);
        for (var roundType : AssessmentGradingRoundType.values()) {
            all.addAll(assignableStatuses(roundType));
        }
        return all;
    }

    /** Hành động hợp lệ của vòng. {@code DECLINED} hợp lệ ở mọi vòng nên không nằm đây. */
    public static Set<AssessmentGradingOutcome> allowedOutcomes(AssessmentGradingRoundType roundType) {
        return switch (roundType) {
            case INITIAL, SPOT_CHECK -> EnumSet.of(
                AssessmentGradingOutcome.UPHELD, AssessmentGradingOutcome.REGRADED, AssessmentGradingOutcome.INVALIDATED);
            case REMEDIATION -> EnumSet.of(AssessmentGradingOutcome.UPHELD, AssessmentGradingOutcome.CLEARED_INVALID);
            case APPEAL -> EnumSet.of(AssessmentGradingOutcome.UPHELD, AssessmentGradingOutcome.REGRADED);
        };
    }

    public static boolean isAssignable(AssessmentGradingRoundType roundType, AssessmentCandidateResultStatus status) {
        return status != null && assignableStatuses(roundType).contains(status);
    }

    public static boolean isAllowed(AssessmentGradingRoundType roundType, AssessmentGradingOutcome outcome) {
        return outcome == AssessmentGradingOutcome.DECLINED || allowedOutcomes(roundType).contains(outcome);
    }

    /**
     * Trạng thái bài sau hành động. {@code null} nghĩa là <em>giữ nguyên</em> — dùng
     * cho hậu kiểm (bài không nhấp nháy) và cho REMEDIATION/UPHELD (xác nhận vi phạm).
     *
     * @throws IllegalStateException khi hành động không hợp lệ với vòng; gọi
     *         {@link #isAllowed} trước để báo lỗi tiếng Việt đúng ngữ cảnh.
     */
    public static AssessmentCandidateResultStatus resultStatusAfter(
            AssessmentGradingRoundType roundType, AssessmentGradingOutcome outcome) {
        if (outcome == AssessmentGradingOutcome.DECLINED) {
            return null;
        }
        if (!isAllowed(roundType, outcome)) {
            throw new IllegalStateException(
                "Hành động " + outcome + " không hợp lệ với vòng chấm " + roundType + ".");
        }
        return switch (roundType) {
            case INITIAL -> outcome == AssessmentGradingOutcome.INVALIDATED
                ? AssessmentCandidateResultStatus.INVALID
                : AssessmentCandidateResultStatus.RELEASED;
            // Hậu kiểm: bài đã RELEASED thì giữ RELEASED (null = không đụng), trừ khi
            // kết luận vi phạm.
            case SPOT_CHECK -> outcome == AssessmentGradingOutcome.INVALIDATED
                ? AssessmentCandidateResultStatus.INVALID
                : null;
            case REMEDIATION -> outcome == AssessmentGradingOutcome.CLEARED_INVALID
                ? AssessmentCandidateResultStatus.PENDING_REVIEW
                : null;
            case APPEAL -> AssessmentCandidateResultStatus.RELEASED;
        };
    }

    /** Hành động bắt buộc phải kèm lý do — thiếu là không giải trình được về sau. */
    public static boolean requiresReason(AssessmentGradingOutcome outcome) {
        return outcome == AssessmentGradingOutcome.INVALIDATED
            || outcome == AssessmentGradingOutcome.CLEARED_INVALID
            || outcome == AssessmentGradingOutcome.DECLINED;
    }

    /** Hành động bắt buộc phải kèm điểm mới. */
    public static boolean requiresScores(AssessmentGradingOutcome outcome) {
        return outcome == AssessmentGradingOutcome.REGRADED;
    }
}
