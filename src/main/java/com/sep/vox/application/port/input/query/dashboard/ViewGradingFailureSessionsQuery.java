package com.sep.vox.application.port.input.query.dashboard;

import java.time.Instant;

import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;

/**
 * @param dateFrom  the start, INCLUSIVE; empty = go back
 *                  {@value ViewPlatformOperationalHealthUseCase#DEFAULT_WINDOW_DAYS} days from the end
 * @param dateTo    the end, EXCLUSIVE; empty = right now
 * @param signature the cause group's signature. {@code null} is NOT "no filter" but selects exactly the
 *                  "unknown cause" group -- that group has a null signature, and it is a real group
 */
public record ViewGradingFailureSessionsQuery(
    Instant dateFrom,
    Instant dateTo,
    String signature,
    int page,
    int size
) {
}
