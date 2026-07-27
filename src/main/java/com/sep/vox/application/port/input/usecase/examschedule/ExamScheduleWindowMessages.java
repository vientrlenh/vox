package com.sep.vox.application.port.input.usecase.examschedule;

import com.sep.vox.domain.model.exam.Exam;

/**
 * Thông báo lỗi dùng chung cho các ràng buộc khung giờ ca thi, để tạo và sửa ca thi
 * báo cùng một message.
 */
final class ExamScheduleWindowMessages {

    private ExamScheduleWindowMessages() {
    }

    static String outsideExamWindow(Exam exam) {
        var message = new StringBuilder("Ca thi phải nằm trong khoảng thời gian mở và đóng của kỳ thi");
        if (exam.getOpenAt() != null) {
            message.append(", mở lúc ").append(exam.getOpenAt());
        }
        if (exam.getCloseAt() != null) {
            message.append(", đóng lúc ").append(exam.getCloseAt());
        }
        return message.toString();
    }
}
