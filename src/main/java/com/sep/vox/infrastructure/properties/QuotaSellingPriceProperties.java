package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tỷ giá USD→VND dùng để FE tự tính giá gợi ý bán quota (suggestedTokenUnitPriceVnd = usdToVndRate ×
 * (1 + SubscriptionPlan.serviceFeeRatio)) -- xem QuotaPricingService. Đây là config THỊ TRƯỜNG, dùng
 * chung cho mọi gói (khác serviceFeeRatio, lưu riêng theo từng SubscriptionPlan).
 *
 * PLACEHOLDER: giá trị mặc định bên dưới là số giả định, cần cập nhật tay theo tỷ giá thực tế qua
 * .env (VOX_QUOTA_USD_TO_VND_RATE) -- chưa có cơ chế tự động lấy tỷ giá real-time.
 */
@ConfigurationProperties(prefix = "vox.quota.selling-price")
public record QuotaSellingPriceProperties(BigDecimal usdToVndRate) {

    private static final BigDecimal DEFAULT_USD_TO_VND_RATE = new BigDecimal("26000");

    public QuotaSellingPriceProperties {
        usdToVndRate = usdToVndRate == null ? DEFAULT_USD_TO_VND_RATE : usdToVndRate;
    }
}
