package com.sep.vox.domain.repository;

import java.util.UUID;

/** Tổng duration_seconds trả lời thật của 1 session_id, dùng cho QuotaPricingCalibrationService. */
public record SessionDurationAggregate(UUID sessionId, long totalDurationSeconds) {
}
