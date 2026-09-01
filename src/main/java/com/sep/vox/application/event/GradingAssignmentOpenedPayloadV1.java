package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Một giáo viên vừa được GIAO một vòng chấm cụ thể: chấm lần đầu, hậu kiểm, soi lại bài vô
 * hiệu, hoặc phúc khảo.
 *
 * <p>Chỉ phát khi việc do NGƯỜI KHÁC giao (school admin gán tay, gán tự động, chuyển người
 * chấm). Giáo viên tự nhận bài trong hàng đợi bài kiểm tra trên lớp thì không phát: họ vừa
 * bấm nút, báo lại chính việc họ vừa làm chỉ là tiếng ồn.
 *
 * <p>Một event cho MỘT phân công, khác hẳn {@link ExamHumanGradingRequiredPayloadV1} vốn gộp
 * cả bài thi: ở đây mỗi thông báo là một việc đích danh của một người, nên số lượng bằng số
 * việc thật chứ không nhân theo sĩ số.
 *
 * <p>CHỈ sinh thông báo trong app, không gửi mail -- đi chung topic exam-grading-review, nơi
 * consumer mail không đăng ký.
 *
 * @param candidateLabel thứ để giáo viên nhận ra bài, ĐÃ được chọn theo luật ẩn danh ngay lúc
 *        phát: tên học sinh với bài kiểm tra trên lớp, mã bài 8 ký tự với kỳ thi tập trung.
 *        Quyết định ở đây chứ không để consumer tự chọn theo {@code examKind} là có chủ ý --
 *        chấm mù của kỳ thi tập trung không được rò rỉ (xem GradingStudentIdentityQueryTests),
 *        và payload thì nằm lại vĩnh viễn trong cột notifications.payload. Không bao giờ đặt
 *        tên thật vào đây cho kỳ thi tập trung.
 * @param deadlineAt hạn chấm, null với bài trên lớp (không có hạn hành chính)
 */
public record GradingAssignmentOpenedPayloadV1(
    UUID assignmentId,
    UUID teacherId,
    UUID examId,
    String examName,
    ExamKind examKind,
    String roundType,
    String candidateLabel,
    Instant deadlineAt
) {

}
