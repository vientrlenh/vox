package com.sep.vox.domain.repository;

import java.util.UUID;

/** Total real answering duration_seconds of 1 session_id, used by QuotaPricingCalibrationService. */
public record SessionDurationAggregate(UUID sessionId, long totalDurationSeconds) {
}
