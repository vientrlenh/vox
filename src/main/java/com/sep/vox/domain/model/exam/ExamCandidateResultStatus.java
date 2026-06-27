package com.sep.vox.domain.model.exam;

public enum ExamCandidateResultStatus {
    PENDING_REVIEW, // có item đã gán human review
    RELEASED, 
    APPEALED, 
    RE_GRADING, 
    FINAL, 
    INVALID, 
    RETAKE_REQUIRED
}
