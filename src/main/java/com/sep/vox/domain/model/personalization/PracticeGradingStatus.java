package com.sep.vox.domain.model.personalization;

/**
 * The grading lifecycle of ONE practice answer ({@code practice_item_responses.grading_status}).
 *
 * <p>Completely different from the session layer: {@code practice_sessions.status} (IN_PROGRESS / COMPLETED / ABANDONED)
 * describes THE LEARNER'S BEHAVIOR -- "did they speak" -- and not the grading progress. The two are deliberately separated,
 * see the long note in {@code EndPracticeSessionUseCase}.
 *
 * <p>The naming follows {@code AssessmentSessionStatus} on the assessment side so the whole repo shares one vocabulary. {@link #GRADING_FAILED}
 * in particular must keep the full prefix: {@code AssessmentCandidateResultStatus.FAILED} in this
 * repo means THE STUDENT FAILED, so naming a technical error {@code FAILED} invites later readers
 * to misunderstand in the worst direction.
 */
public enum PracticeGradingStatus {

    PENDING,

    GRADING,

    GRADED,

    GRADING_FAILED;

    /** Đã chốt, không còn gì để chờ nữa. */
    public boolean isTerminal() {
        return this == GRADED;
    }
}
