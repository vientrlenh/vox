package com.sep.vox.domain.dto;

import java.math.BigDecimal;

/**
 * Cảnh báo (KHÔNG chặn) ước lượng worst-case chi phí AI + hạn mức còn lại -- xem
 * ClassTestTokenQuotaGuardService.estimateTokenQuota. remainingClassTestUsd/wouldExceedClassTest
 * chỉ có ý nghĩa với bài CLASS_TEST; remaining* = null nếu trường chưa có subscription active hoặc
 * chưa cấu hình hạn mức loại đó.
 */
public record ExamTokenEstimateDto(
    BigDecimal estimatedCostUsd,
    BigDecimal remainingGradingUsd,
    BigDecimal remainingClassTestUsd,
    boolean wouldExceedGrading,
    boolean wouldExceedClassTest
) {
}
