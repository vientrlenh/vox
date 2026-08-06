package com.sep.vox.domain.repository.personalization;

import java.util.UUID;

public interface StudentQuestionExposureRepository {

    void recordExposure(UUID studentId, UUID questionId);

    /**
     * Gỡ dấu "đã gặp" của một câu -- dùng khi câu được CHỌN nhưng học sinh chưa bao giờ trả lời
     * (xem {@code UndeliveredQuestionCleanupService}).
     *
     * <p>Đây là bảng quyết định câu nào còn được chào lại: mọi truy vấn chọn câu đều lọc
     * {@code exposure.id IS NULL}. Không gỡ thì một câu chỉ nạp trước rồi bỏ dở sẽ biến mất
     * khỏi kho của học sinh vĩnh viễn.
     */
    void removeExposure(UUID studentId, UUID questionId);
}
