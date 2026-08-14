package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Tham số cho QuotaPricingCalibrationService -- cửa sổ dữ liệu, cỡ mẫu tối thiểu và biên an toàn
 * khi tự tính lại giá mỗi giây. Giá trị thật bind từ application.yaml
 * ({@code vox.quota.calibration.*}) ở QuotaPricingCalibrationProperties.
 */
public interface QuotaPricingCalibrationConfigPort {

    Integer windowDays();

    Integer minSampleSessions();

    BigDecimal maxChangeRatio();

    BigDecimal minRateBound();

    BigDecimal maxRateBound();
}
