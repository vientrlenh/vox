package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tỷ giá USD→VND dùng để tính tokenUnitPrice = usdToVndRate × (1 + SubscriptionPlan.serviceFeeRatio)
 * -- xem QuotaPricingService/CreatePlanUseCase/UpdatePlanUseCase. Đây là config THỊ TRƯỜNG, dùng
 * chung cho mọi gói (khác serviceFeeRatio, lưu riêng theo từng SubscriptionPlan).
 *
 * <p>Chỉ còn là FALLBACK TĨNH: nguồn đọc chính là snapshot mới nhất do ExchangeRateRefreshJob tự
 * fetch từ API tỷ giá thật (xem ExchangeRateApiProperties/QuotaPricingService.usdToVndRate()) --
 * giá trị ở đây (VOX_QUOTA_USD_TO_VND_RATE) chỉ dùng khi chưa có snapshot nào (mới deploy, job
 * chưa chạy lần nào).
 */
@ConfigurationProperties(prefix = "vox.quota.selling-price")
public record QuotaSellingPriceProperties(BigDecimal usdToVndRate) {

    private static final BigDecimal DEFAULT_USD_TO_VND_RATE = new BigDecimal("26000");

    public QuotaSellingPriceProperties {
        usdToVndRate = usdToVndRate == null ? DEFAULT_USD_TO_VND_RATE : usdToVndRate;
    }
}
