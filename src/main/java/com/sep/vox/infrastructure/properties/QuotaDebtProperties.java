package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Trần cảnh báo cho nợ hạn mức AI (usedQuantity vượt totalAllocated -- xem
 * SchoolSubscriptionDebtGuardService). CHỈ để cảnh báo sớm khi pipeline đo chi phí AI có bug làm
 * nợ tăng bất thường -- KHÔNG chặn việc trừ quota (chi phí thật luôn phải được ghi nhận đúng) và
 * KHÔNG ảnh hưởng cơ chế tự mở khóa khi trường trả hết nợ.
 */
@ConfigurationProperties(prefix = "vox.quota.debt")
public record QuotaDebtProperties(
    BigDecimal capRatio
) {

    private static final BigDecimal DEFAULT_CAP_RATIO = new BigDecimal("0.20");

    public QuotaDebtProperties {
        capRatio = capRatio == null ? DEFAULT_CAP_RATIO : capRatio;
    }
}
