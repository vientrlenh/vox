package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

/** @param from bao gồm, {@code to} KHÔNG bao gồm -- hai kỳ liền nhau không đếm trùng mốc giao. */
public record ViewSchoolBalanceSummaryQuery(UUID schoolId, Instant from, Instant to) {
}
