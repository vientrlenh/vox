package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Tổng cost_usd của 1 exam_session_id, dùng cho QuotaPricingCalibrationService. */
public record SessionCostAggregate(UUID sessionId, BigDecimal totalCostUsd) {
}
