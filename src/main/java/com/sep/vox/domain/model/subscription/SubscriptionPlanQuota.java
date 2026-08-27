package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Định mức một loại hạn mức được bao gồm trong giá gói, tính bằng VND.
 *
 * <p>Không còn tokenUnitPrice: đơn giá "VND cho mỗi $1 hạn mức" trước đây là tỷ giá ĐÃ CỘNG LÃI
 * (fx × (1 + serviceFeeRatio)), khiến trường nhìn thấy một tỷ giá không khớp thị trường. Phần lãi
 * giờ là một dòng riêng trên đơn hàng, còn quy đổi USD→VND ghi theo từng lượt dùng ở
 * SchoolBalanceEntry (costUsd + fxRateUsed).
 */
public class SubscriptionPlanQuota {
    private UUID id;
    private UUID subscriptionPlanId;
    private QuotaType quotaType;
    private BigDecimal includedAmountVnd;

    public SubscriptionPlanQuota() {}

    public SubscriptionPlanQuota(UUID id, UUID subscriptionPlanId, QuotaType quotaType, BigDecimal includedAmountVnd) {
        this.id = id;
        this.subscriptionPlanId = subscriptionPlanId;
        this.quotaType = quotaType;
        this.includedAmountVnd = includedAmountVnd;
    }

    public SubscriptionPlanQuota(UUID subscriptionPlanId, QuotaType quotaType, BigDecimal includedAmountVnd) {
        this.subscriptionPlanId = subscriptionPlanId;
        this.quotaType = quotaType;
        this.includedAmountVnd = includedAmountVnd;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getIncludedAmountVnd() {
        return includedAmountVnd;
    }

    public void setIncludedAmountVnd(BigDecimal includedAmountVnd) {
        this.includedAmountVnd = includedAmountVnd;
    }
}
