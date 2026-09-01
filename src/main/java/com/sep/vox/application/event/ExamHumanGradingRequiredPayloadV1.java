package com.sep.vox.application.event;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Bài thi vừa đóng mà còn bài ở {@code PENDING_REVIEW} ("Chờ soát điểm AI") -- cần người vào
 * chấm tay.
 *
 * <p>CHỈ sinh thông báo trong app, KHÔNG gửi mail: đây là lời nhắc công việc lặp lại theo từng
 * bài thi, không phải chứng từ hay tin nhắn tới người ngoài phiên đăng nhập. Vì vậy event đi
 * trên một topic riêng mà chỉ consumer notification đọc -- xem
 * {@code consumer-groups.notification.topic.exam-grading-review} trong application.yaml.
 *
 * <p>Một event cho CẢ bài thi, không phải mỗi bài một event: sĩ số một kỳ thi là hàng trăm,
 * và một thông báo mỗi bài chấm là cách nhanh nhất để người ta tắt hẳn nhóm thông báo này.
 *
 * @param recipientIds chốt ngay lúc phát, không để consumer truy vấn lại -- cùng lý do với
 *        {@link InvoicePaidPayloadV1}: chạy lại (retry, replay từ DLT) phải ra đúng tập người
 *        nhận cũ thì uk_notifications_user_event mới chặn được trùng
 * @param examKind quyết định client mở hàng đợi chấm của bài trên lớp hay của kỳ tập trung
 * @param pendingCount số bài đang chờ chấm tay tại lúc đóng bài
 */
public record ExamHumanGradingRequiredPayloadV1(
    List<UUID> recipientIds,
    UUID examId,
    String examName,
    ExamKind examKind,
    int pendingCount
) {

}
