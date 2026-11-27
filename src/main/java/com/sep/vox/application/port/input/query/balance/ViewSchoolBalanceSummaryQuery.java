package com.sep.vox.application.port.input.query.balance;

import java.time.Instant;
import java.util.UUID;

/** @param from inclusive, {@code to} EXCLUSIVE -- two adjacent periods do not count the boundary instant twice. */
public record ViewSchoolBalanceSummaryQuery(UUID schoolId, Instant from, Instant to) {
}
