package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Ngưỡng cảnh báo SỚM khi hạn mức AI của trường mới dùng tới một tỉ lệ nào đó của
 * {@code total_allocated_amount_vnd} -- xem ConsumeQuotaService.checkUsageWarningTransition. Giá trị
 * thật bind từ application.yaml ({@code vox.quota.usage-warning.*}) ở QuotaUsageWarningProperties.
 */
public interface QuotaUsageWarningConfigPort {

    /**
     * Fires a warning when {@code usedAmountVnd} has just crossed {@code totalAllocatedAmountVnd * warningRatio}
     * of exactly the quota wallet (ASSESSMENT or PRACTICE) that was just debited. It is taken as a RATIO of the quota, for the same reason as
     * {@link QuotaDebtConfigPort#capRatio()}: schools that buy a large/small plan need the same relative warning threshold.
     */
    BigDecimal warningRatio();
}
