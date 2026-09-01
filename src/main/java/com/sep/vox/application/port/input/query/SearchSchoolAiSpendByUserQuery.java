package com.sep.vox.application.port.input.query;

import java.time.Instant;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Cửa sổ và bộ lọc của bảng "ai đang tiêu hạn mức".
 *
 * <p>Cùng quy ước cửa sổ nửa mở {@code [from, to)} với {@link ViewSchoolAiCostQuery} — bảng này đứng
 * ngay dưới biểu đồ nên hai bên phải đọc cùng một khoảng, nếu không tổng bảng và tổng biểu đồ lệch
 * nhau mà không có lời giải thích nào.
 *
 * @param quotaType null = cả hai loại ví.
 */
public record SearchSchoolAiSpendByUserQuery(
    Instant from,
    Instant to,
    QuotaType quotaType,
    int page,
    int size
) {
}
