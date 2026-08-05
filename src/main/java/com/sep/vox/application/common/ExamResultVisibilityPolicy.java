package com.sep.vox.application.common;

import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;

/**
 * Khi nào CHÍNH THÍ SINH được xem kết quả của mình.
 *
 * <p>Bốn trạng thái bị ẩn ({@code PENDING_REVIEW}, {@code APPEALED}, {@code RE_GRADING},
 * {@code RETAKE_REQUIRED}) đều là "điểm chưa chốt hoặc đang đổi": cho học sinh xem lúc này
 * là công bố một con số sắp thay đổi, rồi phải rút lại.
 *
 * <p>Giáo viên / school admin / system admin KHÔNG đi qua luật này — họ cần thấy điểm để
 * có căn cứ mà chấm. Luật này chỉ áp cho người gọi chính là thí sinh của bài.
 */
public final class ExamResultVisibilityPolicy {

    private ExamResultVisibilityPolicy() {
    }

    /**
     * {@code switch} vét cạn, cố ý KHÔNG có {@code default}: thêm giá trị mới vào
     * {@link ExamCandidateResultStatus} là lỗi biên dịch ngay tại đây, buộc người thêm phải
     * quyết định học sinh có được xem hay không — thay vì im lặng rơi vào một nhánh mặc định
     * mà không ai đọc lại. Cùng khuôn với {@code ResolveExamCandidateAttemptsUseCase}.
     */
    public static boolean isVisibleToCandidate(ExamCandidateResultStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case RELEASED, FINAL, PASSED, FAILED, INVALID -> true;
            case PENDING_REVIEW, APPEALED, RE_GRADING, RETAKE_REQUIRED -> false;
        };
    }
}
