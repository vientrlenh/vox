package com.sep.vox.domain.model.exam;

public enum ExamCandidateResultStatus {
    PENDING_REVIEW, // có item đã gán human review
    RELEASED,
    APPEALED,
    RE_GRADING,
    // FINAL cũng là 1 trạng thái CHUNG THẨM riêng: khi kỳ thi RESULTS_PUBLISHED mà
    // assessmentPolicy không có passingScore (không có ngưỡng để tự so sánh), RELEASED
    // chốt thành FINAL thay vì PASSED/FAILED. Từ đó nhà trường tự quyết định PASSED/FAILED
    // thủ công qua decideExamCandidateResultOutcome (xem DecideExamCandidateResultOutcomeUseCase).
    FINAL,
    INVALID,
    RETAKE_REQUIRED,
    PASSED, // chốt sau khi kỳ thi RESULTS_PUBLISHED, điểm >= passingScore (hoặc nhà trường tự chọn từ FINAL)
    FAILED, // chốt sau khi kỳ thi RESULTS_PUBLISHED, điểm < passingScore hoặc INVALID (hoặc nhà trường tự chọn từ FINAL)
    /** Phiên thi sinh ra kết quả này đã bị xoá mềm — xem {@link ExamSessionStatus#DELETED}. */
    DELETED;

    public static boolean isVisibleToCandidate(ExamCandidateResultStatus status) {
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
