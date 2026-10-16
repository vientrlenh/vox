package com.sep.vox.domain.model.metering;

/**
 * Nguồn dữ liệu dùng để calibrate estimatedCostPerSecondUsd (xem QuotaPricingCalibrationService).
 * KHÔNG phải QuotaType (ASSESSMENT/PRACTICE) -- trước đây QuotaType có 3 giá trị và GRADING/CLASS_TEST cùng dùng
 * chung 1 rate ASSESSMENT (công thức guard giống hệt nhau cho cả 2 kind), chỉ PRACTICE tách riêng vì
 * pipeline AI khác hẳn (realtimeCorrectionGraph nhẹ hơn evalGraph).
 */
public enum QuotaPricingSource {
    ASSESSMENT,
    PRACTICE
}
