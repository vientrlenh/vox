package com.sep.vox.domain.model.metering;

/**
 * The data source used to calibrate estimatedCostPerSecondUsd (see QuotaPricingCalibrationService).
 * NOT QuotaType (ASSESSMENT/PRACTICE) -- QuotaType used to have 3 values and GRADING/CLASS_TEST shared
 * 1 ASSESSMENT rate (the guard formula is identical for both kinds), and only PRACTICE was separate because its
 * AI pipeline is very different (realtimeCorrectionGraph is lighter than evalGraph).
 */
public enum QuotaPricingSource {
    ASSESSMENT,
    PRACTICE
}
