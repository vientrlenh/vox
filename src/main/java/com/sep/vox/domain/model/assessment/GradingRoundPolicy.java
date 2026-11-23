package com.sep.vox.domain.model.assessment;

import java.util.EnumSet;
import java.util.Set;

/**
 * The matrix of 4 rounds x 4 actions -- a pure business rule, depending on nothing.
 *
 * <table>
 *   <caption>Grading round x action</caption>
 *   <tr><th>Round</th><th>Attempt is in</th><th>UPHELD</th><th>REGRADED</th>
 *       <th>INVALIDATED</th><th>CLEARED_INVALID</th></tr>
 *   <tr><td>INITIAL</td><td>PENDING_REVIEW</td><td>→ RELEASED</td><td>→ RELEASED</td>
 *       <td>→ INVALID</td><td>—</td></tr>
 *   <tr><td>SPOT_CHECK</td><td>RELEASED</td><td>stays RELEASED</td><td>stays RELEASED, score changes</td>
 *       <td>→ INVALID</td><td>—</td></tr>
 *   <tr><td>REMEDIATION</td><td>INVALID</td><td>stays INVALID</td><td>—</td>
 *       <td>—</td><td>→ PENDING_REVIEW</td></tr>
 *   <tr><td>APPEAL</td><td>APPEALED / RE_GRADING</td><td>→ RELEASED</td><td>→ RELEASED</td>
 *       <td>—</td><td>—</td></tr>
 * </table>
 *
 * <p>A RELEASED attempt that is post-checked <em>stays</em> RELEASED throughout: the progress lives
 * on the assignment row and not in {@link AssessmentCandidateResultStatus}. That way no value
 * has to be added to that enum -- avoiding the trap that {@code ddl-auto} cannot alter a
 * CHECK constraint.
 */
public final class GradingRoundPolicy {

    private GradingRoundPolicy() {}

    /** Trạng thái bài được phép nhận một phân công của vòng này. */
    public static Set<AssessmentCandidateResultStatus> assignableStatuses(GradingRoundType roundType) {
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
        Set<AssessmentCandidateResultStatus> all = EnumSet.noneOf(AssessmentCandidateResultStatus.class);
        for (GradingRoundType roundType : GradingRoundType.values()) {
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

    public static boolean isAssignable(GradingRoundType roundType, AssessmentCandidateResultStatus status) {
        return status != null && assignableStatuses(roundType).contains(status);
    }

    public static boolean isAllowed(GradingRoundType roundType, GradingOutcome outcome) {
        return outcome == GradingOutcome.DECLINED || allowedOutcomes(roundType).contains(outcome);
    }

    /**
     * Result status after the action. {@code null} means <em>keep as is</em> -- used for
     * post-review (the result does not flicker) and for REMEDIATION/UPHELD (violation confirmed).
     *
     * @throws IllegalArgumentException when the action is not valid for the round; call
     *         {@link #isAllowed} first to report the error in Vietnamese with the right context.
     */
    public static AssessmentCandidateResultStatus resultStatusAfter(
            GradingRoundType roundType, GradingOutcome outcome) {
        if (outcome == GradingOutcome.DECLINED) {
            return null;
        }
        if (!isAllowed(roundType, outcome)) {
            throw new IllegalArgumentException(
                "Hành động " + outcome + " không hợp lệ với vòng chấm " + roundType + ".");
        }
        return switch (roundType) {
            case INITIAL -> outcome == GradingOutcome.INVALIDATED
                ? AssessmentCandidateResultStatus.INVALID
                : AssessmentCandidateResultStatus.RELEASED;
            // Hậu kiểm: bài đã RELEASED thì giữ RELEASED (null = không đụng), trừ khi
            // kết luận vi phạm.
            case SPOT_CHECK -> outcome == GradingOutcome.INVALIDATED
                ? AssessmentCandidateResultStatus.INVALID
                : null;
            case REMEDIATION -> outcome == GradingOutcome.CLEARED_INVALID
                ? AssessmentCandidateResultStatus.PENDING_REVIEW
                : null;
            case APPEAL -> AssessmentCandidateResultStatus.RELEASED;
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
