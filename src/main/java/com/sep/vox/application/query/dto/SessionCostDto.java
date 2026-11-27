package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The total cost_usd of ONE session -- just one row of the price-calibration report, not a snapshot of any
 * aggregate. That is why it lives in query/dto and not in the domain: QuotaPricingCalibrationService reads
 * it to derive the estimated price per second, and no command writes it back in this shape.
 */
public record SessionCostDto(UUID sessionId, BigDecimal totalCostUsd) {
}
