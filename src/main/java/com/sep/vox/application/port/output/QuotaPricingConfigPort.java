package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Hằng số giá tĩnh trong .env mà application cần đọc -- là mức FALLBACK khi chưa có lần calibrate
 * nào thành công. Giá trị thật bind từ application.yaml ({@code vox.quota.*}) ở
 * QuotaPricingProperties.
 *
 * <p>Khác QuotaPricingPort: port kia trả về giá ĐANG áp dụng (đã chọn giữa số calibrate và số tĩnh),
 * còn interface này chỉ đưa ra số tĩnh thô cho QuotaPricingCalibrationService làm mốc so sánh.
 */
public interface QuotaPricingConfigPort {

    BigDecimal estimatedCostPerExamSecondUsd();

    BigDecimal estimatedCostPerPracticeSecondUsd();
}
