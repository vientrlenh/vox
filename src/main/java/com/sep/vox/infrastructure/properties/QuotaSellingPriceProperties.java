package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hai tham số để ra giá bán hạn mức, đọc ĐỘC LẬP với nhau qua QuotaPricingPort -- xem
 * QuotaPricingService.
 *
 * <p>Trước đây hai số này bị nhân sẵn thành MỘT đơn giá (tokenUnitPriceVnd = usdToVndRate ×
 * (1 + serviceFeeRatio)) đóng băng trên từng dòng hạn mức của gói. Đã bỏ hẳn: gộp lại thì trường
 * thấy "1 USD = 31.200đ" trong khi tỷ giá thật là 26.000đ, không đối chiếu được với bất kỳ tỷ giá
 * nào ngoài thị trường. Giờ phí dịch vụ đứng thành một dòng riêng trên đơn hàng, còn quy đổi
 * USD->VND ghi theo từng lượt dùng ở SchoolBalanceEntry (costUsd + fxRateUsed).
 *
 * <p>usdToVndRate chỉ còn là FALLBACK TĨNH: nguồn đọc chính là snapshot mới nhất do
 * ExchangeRateRefreshJob tự fetch từ API tỷ giá thật (xem ExchangeRateApiProperties /
 * QuotaPricingService.usdToVndRate()) -- giá trị ở đây (VOX_QUOTA_USD_TO_VND_RATE) chỉ dùng khi chưa
 * có snapshot nào (mới deploy, job chưa chạy lần nào).
 *
 * <p>serviceFeeRatio thì ngược lại, đây là nguồn DUY NHẤT. Trước kia mỗi SubscriptionPlan giữ một
 * ratio riêng, nhưng cột service_fee_ratio đã bị bỏ khỏi gói: một gói được thay thế
 * (replacedByPlanId) sẽ sinh id mới, nên margin nằm theo gói thì mỗi lần đổi giá lại phải chép tay
 * sang gói mới và rất dễ lệch giữa các gói đang cùng bán.
 */
@ConfigurationProperties(prefix = "vox.quota.selling-price")
public record QuotaSellingPriceProperties(BigDecimal usdToVndRate, BigDecimal serviceFeeRatio) {

    private static final BigDecimal DEFAULT_USD_TO_VND_RATE = new BigDecimal("26000");
    private static final BigDecimal DEFAULT_SERVICE_FEE_RATIO = new BigDecimal("0.05"); // chỉnh thành default 5%, 20% ở thời điểm hiện tại đang quá nhiều so với ước tính

    public QuotaSellingPriceProperties {
        usdToVndRate = usdToVndRate == null ? DEFAULT_USD_TO_VND_RATE : usdToVndRate;
        serviceFeeRatio = serviceFeeRatio == null ? DEFAULT_SERVICE_FEE_RATIO : serviceFeeRatio;
    }
}
