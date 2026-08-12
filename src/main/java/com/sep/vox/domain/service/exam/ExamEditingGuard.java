package com.sep.vox.domain.service.exam;

import com.sep.vox.domain.model.exam.Exam;

/**
 * Khoá chỉnh sửa kỳ thi từ lúc kỳ thi bắt đầu trở đi (xem {@link Exam#isLockedForEditing()}).
 * Gom lại một chỗ để mọi use case tạo/sửa/xoá thông tin kỳ thi và lịch thi báo cùng một thông báo.
 *
 * <p>Các thao tác vận hành trong lúc thi -- công bố/hoàn thành/huỷ ca thi, lấy OTP, điểm danh,
 * chấm bài -- cố ý không dùng guard này.
 */
public final class ExamEditingGuard {

    private ExamEditingGuard() {
    }

    /** Thông tin kỳ thi: tên, mô tả, khung giờ, chính sách đánh giá... */
    public static void requireExamEditable(Exam exam) {
        if (exam.isLockedForEditing()) {
            throw new IllegalStateException("Không thể chỉnh sửa kỳ thi khi kỳ thi đã bắt đầu");
        }
    }

    /** Lịch thi: ca thi, giám thị, xếp thí sinh vào ca, phân đề. */
    public static void requireScheduleEditable(Exam exam) {
        if (exam.isLockedForEditing()) {
            throw new IllegalStateException("Không thể thay đổi lịch thi khi kỳ thi đã bắt đầu");
        }
    }
}
