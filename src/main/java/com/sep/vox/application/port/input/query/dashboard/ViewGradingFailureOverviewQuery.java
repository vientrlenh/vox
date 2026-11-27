package com.sep.vox.application.port.input.query.dashboard;

import java.time.Instant;

import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;
    /**
     * @param dateFrom the start, INCLUSIVE; empty = go back
     *                 {@value ViewPlatformOperationalHealthUseCase#DEFAULT_WINDOW_DAYS} days from the end
     * @param dateTo   the end, EXCLUSIVE; empty = right now
     */
public record ViewGradingFailureOverviewQuery(
    Instant dateFrom, 
    Instant dateTo
) {
    
}
