package com.sep.vox.application.query.dto;

import java.time.Instant;

/**
 * Bài đã thi xong mà học sinh CHƯA có điểm, chia theo thứ đang chặn.
 *
 * <p>Năm nhóm LOẠI TRỪ NHAU và phủ kín — cộng lại đúng bằng {@link #total()}. Đó là điều kiện để thẻ
 * trên trang tổng quan cộng được năm dòng thành một con số mà không đếm trùng ai.
 *
 * <p>KHÔNG dùng {@code gradingStats.unassigned} cho nhóm "chờ phân công" dù nghe giống hệt: bên đó
 * đếm bài ĐỦ ĐIỀU KIỆN nhận thêm một vòng chấm, tập ấy gồm cả RELEASED / INVALID / APPEALED — tức
 * mọi bài đã công bố điểm cũng nằm trong đó. Dùng nhầm là biến "học sinh chưa có điểm" thành "gần
 * như mọi bài của trường".
 */
public record SchoolUnscoredWorkloadDto(
    /** AI chấm lỗi, chưa ai đụng tới, và trường vẫn còn lượt nhờ AI chấm lại. */
    int aiFailedRetryLeft,
    /** AI chấm lỗi, đã dùng hết lượt AI — chỉ còn đường chấm tay. */
    int aiFailedNoRetryLeft,
    /** Đã có dòng kết quả chờ người chấm nhưng chưa phân công cho ai. */
    int awaitingAssignment,
    /** Đã phân công, quá hạn chấm. */
    int assignedOverdue,
    /** Đã phân công, còn trong hạn. */
    int assignedInProgress,
    /** Mốc nộp bài của bài chờ LÂU NHẤT; null khi không còn bài nào chưa có điểm. */
    Instant oldestSubmittedAt,
    /** Số kỳ thi KHÁC NHAU đang có bài chưa ra điểm. */
    int examCount
) {

    public int total() {
        return aiFailedRetryLeft + aiFailedNoRetryLeft + awaitingAssignment + assignedOverdue
            + assignedInProgress;
    }

    /** Tổng số bài AI chấm lỗi chưa ai xử lý — hai nhóm con gộp lại thành một dòng trên giao diện. */
    public int aiFailed() {
        return aiFailedRetryLeft + aiFailedNoRetryLeft;
    }

    public static SchoolUnscoredWorkloadDto empty() {
        return new SchoolUnscoredWorkloadDto(0, 0, 0, 0, 0, null, 0);
    }
}
