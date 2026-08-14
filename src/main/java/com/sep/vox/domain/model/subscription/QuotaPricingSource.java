package com.sep.vox.domain.model.subscription;

/**
 * Nguồn dữ liệu dùng để calibrate estimatedCostPerSecondUsd (xem QuotaPricingCalibrationService).
 * KHÔNG phải QuotaType (GRADING/CLASS_TEST/PRACTICE, 3 giá trị) -- GRADING và CLASS_TEST cùng dùng
 * chung 1 rate EXAM (công thức guard giống hệt nhau cho cả 2 kind), chỉ PRACTICE tách riêng vì
 * pipeline AI khác hẳn (realtimeCorrectionGraph nhẹ hơn evalGraph).
 */
public enum QuotaPricingSource {
    EXAM,
    PRACTICE
}
