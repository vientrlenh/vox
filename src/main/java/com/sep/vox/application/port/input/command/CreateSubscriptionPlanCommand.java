package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * Không còn serviceFeeRatio: phí dịch vụ đã thành config TOÀN HỆ THỐNG
 * (vox.quota.selling-price.service-fee-ratio) và được cộng thành một dòng riêng trên ĐƠN HÀNG, chứ
 * không lưu theo từng gói cũng không gộp vào tỷ giá -- xem {@code QuotaPricingPort#serviceFeeRatio()}.
 *
 * <p>Chu kỳ gói = periodType x periodCount (vd. MONTH x 12) thay cho validityDays cũ: gói tính theo
 * tháng/năm thì ngày hết hạn phải rơi đúng ngày tương ứng, cộng thô theo số ngày sẽ lệch dần.
 */
public record CreateSubscriptionPlanCommand(
    String name,
    String tagline,
    BigDecimal priceVnd,
    String periodType,
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    List<CreateSubscriptionPlanQuotaCommand> quotas
) {
}
