package com.sep.vox.application.query.dto;

/**
 * Ảnh chụp phiên thi tại ĐÚNG thời điểm gọi — không phụ thuộc cửa sổ thời gian đang xem trên
 * dashboard.
 *
 * <p>Tách khỏi {@link GradingOutcomeBucketDto} vì hai con số trả lời hai câu hỏi khác nhau: chuỗi
 * theo ngày là "đường chấm AI có ổn không trong 14 ngày qua", còn bản ghi này là "ngay lúc này đang
 * có gì chạy". Gộp chung một query sẽ buộc một trong hai phải nhận cửa sổ thời gian mà nó không cần.
 */
public record LiveSessionCountsDto(
    long sessionsInProgress,
    long examsInProgress,
    long gradingQueueDepth
) {
}
