package com.sep.vox.application.port.input.query;

import java.time.Instant;

import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;

/**
 * @param dateFrom  mốc đầu BAO GỒM; bỏ trống = lùi
 *                  {@value ViewPlatformOperationalHealthUseCase#DEFAULT_WINDOW_DAYS} ngày từ mốc cuối
 * @param dateTo    mốc cuối KHÔNG bao gồm; bỏ trống = ngay lúc này
 * @param signature chữ ký nhóm nguyên nhân. {@code null} KHÔNG phải "bỏ lọc" mà chọn đúng nhóm
 *                  "không rõ nguyên nhân" — nhóm đó có chữ ký null, và nó là một nhóm thật
 */
public record ViewGradingFailureSessionsQuery(
    Instant dateFrom,
    Instant dateTo,
    String signature,
    int page,
    int size
) {
}
