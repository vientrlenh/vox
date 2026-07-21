package com.sep.vox.domain.model.exam;

public enum ExamCandidateResultStatus {
    PENDING_REVIEW, // có item đã gán human review
    RELEASED,
    APPEALED,
    RE_GRADING,
    FINAL,
    INVALID,
    RETAKE_REQUIRED,
    PASSED, // chốt sau khi kỳ thi RESULTS_PUBLISHED, điểm >= passingScore
    FAILED // chốt sau khi kỳ thi RESULTS_PUBLISHED, điểm < passingScore hoặc INVALID
}
