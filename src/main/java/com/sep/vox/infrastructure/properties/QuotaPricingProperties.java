package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.QuotaPricingConfigPort;

/**
 * Hệ số quy đổi USD dùng để ước lượng worst-case chi phí AI trước khi cho phép lên
 * lịch kỳ thi (xem ClassTestTokenQuotaGuardService). Đây là số ước tính, KHÔNG phải
 * chi phí thật -- chi phí thật đến từ ai_usage_record sau khi Agentic AI xử lý xong.
 *
 * PLACEHOLDER: giá trị mặc định bên dưới là số giả định, cần thay bằng số đo thực tế
 * từ ai_usage_record (trung bình cost_usd / audio_duration_seconds trên một khoảng
 * thời gian đủ dài) trước khi dùng ở production.
 */
@ConfigurationProperties(prefix = "vox.quota")
public record QuotaPricingProperties(
    BigDecimal estimatedCostPerExamSecondUsd,
    // Tách riêng khỏi estimatedCostPerExamSecondUsd -- PRACTICE dùng pipeline AI nhẹ hơn hẳn EXAM
    // (realtimeCorrectionGraph vs evalGraph), xem QuotaPricingCalibrationService/QuotaPricingSource.
    BigDecimal estimatedCostPerPracticeSecondUsd
) implements QuotaPricingConfigPort {

    private static final BigDecimal DEFAULT_ESTIMATED_COST_PER_EXAM_SECOND_USD = new BigDecimal("0.01");
    private static final BigDecimal DEFAULT_ESTIMATED_COST_PER_PRACTICE_SECOND_USD = new BigDecimal("0.01");

    public QuotaPricingProperties {
        estimatedCostPerExamSecondUsd = estimatedCostPerExamSecondUsd == null
            ? DEFAULT_ESTIMATED_COST_PER_EXAM_SECOND_USD
            : estimatedCostPerExamSecondUsd;
        estimatedCostPerPracticeSecondUsd = estimatedCostPerPracticeSecondUsd == null
            ? DEFAULT_ESTIMATED_COST_PER_PRACTICE_SECOND_USD
            : estimatedCostPerPracticeSecondUsd;
    }
}