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

    /** Statuses in which a result may receive an assignment of this round. */
    public static Set<AssessmentCandidateResultStatus> assignableStatuses(GradingRoundType roundType) {
        return switch (roundType) {
            case INITIAL -> EnumSet.of(AssessmentCandidateResultStatus.PENDING_REVIEW);
            case SPOT_CHECK -> EnumSet.of(AssessmentCandidateResultStatus.RELEASED);
            case REMEDIATION -> EnumSet.of(AssessmentCandidateResultStatus.INVALID);
            // An appeal moves the result to APPEALED when created, then RE_GRADING once a grader is
            // assigned — both are accepted so it can be reassigned after changing the teacher.
            case APPEAL -> EnumSet.of(
                AssessmentCandidateResultStatus.APPEALED, AssessmentCandidateResultStatus.RE_GRADING);
        };
    }

    /**
     * The union of {@link #assignableStatuses} over the four rounds — "results that can still receive some
     * grading round". This is the correct denominator for the <em>unassigned</em> figure: results that have
     * finished their lifecycle ({@code FINAL}, {@code PASSED}, {@code FAILED},
     * {@code RETAKE_REQUIRED}…) are not in this set.
     *
     * <p>Derived from the matrix above instead of copied by hand, so that adding a new round makes the figure
     * correct automatically.
     */
    public static Set<AssessmentCandidateResultStatus> allAssignableStatuses() {
        Set<AssessmentCandidateResultStatus> all = EnumSet.noneOf(AssessmentCandidateResultStatus.class);
        for (GradingRoundType roundType : GradingRoundType.values()) {
            all.addAll(assignableStatuses(roundType));
        }
        return all;
    }

    /** Valid actions of the round. {@code DECLINED} is valid in every round so it is not listed here. */
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
                "Action " + outcome + " is not valid for grading round " + roundType + ".");
        }
        return switch (roundType) {
            case INITIAL -> outcome == GradingOutcome.INVALIDATED
                ? AssessmentCandidateResultStatus.INVALID
                : AssessmentCandidateResultStatus.RELEASED;
            // Spot check: a result already RELEASED stays RELEASED (null = do not touch), unless
            // the conclusion is a violation.
            case SPOT_CHECK -> outcome == GradingOutcome.INVALIDATED
                ? AssessmentCandidateResultStatus.INVALID
                : null;
            case REMEDIATION -> outcome == GradingOutcome.CLEARED_INVALID
                ? AssessmentCandidateResultStatus.PENDING_REVIEW
                : null;
            case APPEAL -> AssessmentCandidateResultStatus.RELEASED;
        };
    }

    /** Actions that must come with a reason — without one they cannot be justified later. */
    public static boolean requiresReason(GradingOutcome outcome) {
        return outcome == GradingOutcome.INVALIDATED
            || outcome == GradingOutcome.CLEARED_INVALID
            || outcome == GradingOutcome.DECLINED;
    }

    /** Actions that must come with a new score. */
    public static boolean requiresScores(GradingOutcome outcome) {
        return outcome == GradingOutcome.REGRADED;
    }
}
