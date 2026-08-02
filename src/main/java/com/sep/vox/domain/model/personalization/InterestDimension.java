package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;

/**
 * Một chiều sở thích trong danh mục do SYSTEM_ADMIN quản lý.
 *
 * @param quizEligible có được đem ra hỏi trong quiz sở thích không. Tách riêng với
 *                     {@code active} vì ACADEMIC_EXAM là chiều hệ thống tự gán cho topic
 *                     lấy từ ngân hàng đề -- vẫn dùng để xếp hạng chủ đề, nhưng không phải
 *                     sở thích nên không được hỏi học sinh.
 */
public record InterestDimension(
    String code,
    String label,
    String description,
    boolean active,
    boolean quizEligible,
    int displayOrder,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
}
