package com.sep.vox.domain.model.assessment;

public enum AssessmentCandidateResultStatus {
    PENDING_REVIEW, // có item đã gán human review
    RELEASED,
    APPEALED,
    RE_GRADING,
    // FINAL is also a separate JUDGMENT state: when the assessment is RESULTS_PUBLISHED and
    // assessmentPolicy has no passingScore (no threshold to compare against automatically), RELEASED
    // is finalized to FINAL instead of PASSED/FAILED. From then on the school decides PASSED/FAILED
    // manually through decideAssessmentCandidateResultOutcome (see DecideAssessmentCandidateResultOutcomeUseCase).
    FINAL,
    INVALID,
    RETAKE_REQUIRED,
    PASSED, // chốt sau khi kỳ thi RESULTS_PUBLISHED, điểm >= passingScore (hoặc nhà trường tự chọn từ FINAL)
    FAILED, // chốt sau khi kỳ thi RESULTS_PUBLISHED, điểm < passingScore hoặc INVALID (hoặc nhà trường tự chọn từ FINAL)
    /** The assessment session that produced this result has been soft-deleted -- see {@link AssessmentSessionStatus#DELETED}. */
    DELETED;

    public static boolean isVisibleToCandidate(AssessmentCandidateResultStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case RELEASED, FINAL, PASSED, FAILED, INVALID -> true;
            // DELETED: kết quả của phiên đã xoá — học sinh tuyệt đối không được thấy lại.
            case PENDING_REVIEW, APPEALED, RE_GRADING, RETAKE_REQUIRED, DELETED -> false;
        };
    }
}
