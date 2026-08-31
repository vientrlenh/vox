package com.sep.vox.application.port.input.query;

import java.time.Instant;

import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;
    /**
     * @param dateFrom mốc đầu BAO GỒM; bỏ trống = lùi
     *                 {@value ViewPlatformOperationalHealthUseCase#DEFAULT_WINDOW_DAYS} ngày từ mốc cuối
     * @param dateTo   mốc cuối KHÔNG bao gồm; bỏ trống = ngay lúc này
     */
public record ViewGradingFailureOverviewQuery(
    Instant dateFrom, 
    Instant dateTo
) {
    
}
