package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một kỳ thi nhìn từ màn GIÁM SÁT -- cố ý nhỏ hơn nhiều so với {@code ExamDto}.
 *
 * <p>Tách khỏi DTO quản lý kỳ thi vì hai màn có hai tập người xem khác nhau. Giám thị được phân công
 * một ca thi cần đúng chừng này để nhận ra phòng của mình; họ không cần (và không được) thấy
 * blueprint, chính sách chấm, ngưỡng AI hay quyền stream mà {@code ExamDto} mang theo. Dùng chung
 * một DTO nghĩa là mỗi trường thêm vào cho màn quản lý về sau đều tự động chảy sang cho giám thị.
 *
 * @param windowStart mốc bắt đầu SỚM NHẤT trong các ca mà người xem giám sát được, không phải
 *                    {@code openAt} của kỳ thi -- kỳ thi có thể mở cả ngày trong khi ca của người
 *                    này chỉ chạy một tiếng.
 * @param liveScheduleCount số ca đang chạy NGAY BÂY GIỜ. Bằng 0 nghĩa là kỳ thi lọt vào danh sách vì
 *                    sắp bắt đầu, và màn hình phải nói vậy thay vì mời bấm vào xem trực tiếp.
 */
public record MonitoredExamSummary(
    UUID examId,
    String code,
    String name,
    String kind,
    String status,
    Instant windowStart,
    Instant windowEnd,
    long liveScheduleCount
) {
}
