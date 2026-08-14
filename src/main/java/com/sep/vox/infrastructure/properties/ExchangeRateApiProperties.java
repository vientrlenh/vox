package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cho ExchangeRateApiClient/ExchangeRateRefreshJob -- job tự động lấy tỷ giá USD->VND
 * thật từ API bên ngoài định kỳ, thay cho hằng số tĩnh gõ tay ở QuotaSellingPriceProperties.
 *
 * <p>minRateBound/maxRateBound là chặn an toàn: nếu API trả về giá trị ngoài khoảng này (lỗi
 * provider, response dị dạng, ...) thì ExchangeRateApiClient coi như fetch thất bại và KHÔNG lưu
 * snapshot -- tránh 1 lần API trả rác làm hỏng giá bán quota thật (xem CreatePlanUseCase/
 * UpdatePlanUseCase tự tính tokenUnitPrice từ tỷ giá này).
 */
@ConfigurationProperties(prefix = "vox.quota.exchange-rate-api")
public record ExchangeRateApiProperties(String baseUrl, BigDecimal minRateBound, BigDecimal maxRateBound) {

    private static final String DEFAULT_BASE_URL = "https://open.er-api.com/v6/latest/USD";
    private static final BigDecimal DEFAULT_MIN_RATE_BOUND = new BigDecimal("15000");
    private static final BigDecimal DEFAULT_MAX_RATE_BOUND = new BigDecimal("40000");

    public ExchangeRateApiProperties {
        baseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl;
        minRateBound = minRateBound == null ? DEFAULT_MIN_RATE_BOUND : minRateBound;
        maxRateBound = maxRateBound == null ? DEFAULT_MAX_RATE_BOUND : maxRateBound;
    }
}
