package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tổng cost_usd của MỘT phiên -- chỉ là một dòng của báo cáo calibrate giá, không phải ảnh chụp của
 * aggregate nào. Vì thế nó nằm ở query/dto chứ không phải domain: QuotaPricingCalibrationService đọc
 * nó để suy ra giá ước tính mỗi giây, và không có lệnh nào ghi lại theo hình dạng này.
 */
public record SessionCostDto(UUID sessionId, BigDecimal totalCostUsd) {
}
