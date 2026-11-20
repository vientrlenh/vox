package com.sep.vox.domain.service.assessment;

import com.sep.vox.domain.model.assessment.Assessment;

/**
 * Locks editing of an assessment from the moment it starts (see {@link Assessment#isLockedForEditing()}).
 * Gathered in one place so every use case that creates/edits/deletes assessment information and schedules reports the same message.
 *
 * <p>Operational actions during the assessment -- publishing/completing/cancelling a session, getting the OTP, attendance,
 * grading -- deliberately do not use this guard.
 */
public final class AssessmentEditingGuard {

    private AssessmentEditingGuard() {
    }

    /** Thông tin kỳ thi: tên, mô tả, khung giờ, chính sách đánh giá... */
    public static void requireAssessmentEditable(Assessment assessment) {
        if (assessment.isLockedForEditing()) {
            throw new IllegalStateException("Không thể chỉnh sửa kỳ thi khi kỳ thi đã bắt đầu");
        }
    }

    /** Lịch thi: ca thi, giám thị, xếp thí sinh vào ca, phân đề. */
    public static void requireScheduleEditable(Assessment assessment) {
        if (assessment.isLockedForEditing()) {
            throw new IllegalStateException("Không thể thay đổi lịch thi khi kỳ thi đã bắt đầu");
        }
    }
}
