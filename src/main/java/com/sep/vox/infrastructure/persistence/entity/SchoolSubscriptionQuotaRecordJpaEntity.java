package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_subscription_quota_records")
public class SchoolSubscriptionQuotaRecordJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "school_subscription_id", nullable = false, updatable = false)
    private UUID schoolSubscriptionId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_records_quota_type_valid",
            constraint = "quota_type IN ('EXAM', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "total_allocated_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal totalAllocatedAmountVnd;

    @Column(name = "used_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal usedAmountVnd;

    // Phần của total đến từ ví tự nạp thay vì từ gói -- xem V12. Luôn tăng CÙNG total trong một câu
    // UPDATE (addFundingFromBalance), nên hai cột không lệch được; CHECK ở DB giữ 0 <= funded <= total.
    @Column(name = "funded_from_balance_vnd", nullable = false, precision = 18, scale = 6, check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_records_funded_within_total",
            constraint = "funded_from_balance_vnd >= 0 AND funded_from_balance_vnd <= total_allocated_amount_vnd"
        )
    })
    private BigDecimal fundedFromBalanceVnd;

    protected SchoolSubscriptionQuotaRecordJpaEntity() {}

    public SchoolSubscriptionQuotaRecordJpaEntity(UUID id, UUID schoolSubscriptionId, String quotaType,
            BigDecimal totalAllocatedAmountVnd, BigDecimal usedAmountVnd, BigDecimal fundedFromBalanceVnd) {
        this.id = id;
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
        this.usedAmountVnd = usedAmountVnd;
        this.fundedFromBalanceVnd = fundedFromBalanceVnd == null ? BigDecimal.ZERO : fundedFromBalanceVnd;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolSubscriptionId() {
        return schoolSubscriptionId;
    }

    public void setSchoolSubscriptionId(UUID schoolSubscriptionId) {
        this.schoolSubscriptionId = schoolSubscriptionId;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getTotalAllocatedAmountVnd() {
        return totalAllocatedAmountVnd;
    }

    public void setTotalAllocated(BigDecimal totalAllocatedAmountVnd) {
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
    }

    public BigDecimal getUsedAmountVnd() {
        return usedAmountVnd;
    }

    public void setUsedAmountVnd(BigDecimal usedAmountVnd) {
        this.usedAmountVnd = usedAmountVnd;
    }

    public BigDecimal getFundedFromBalanceVnd() {
        return fundedFromBalanceVnd;
    }

    public void setFundedFromBalanceVnd(BigDecimal fundedFromBalanceVnd) {
        this.fundedFromBalanceVnd = fundedFromBalanceVnd;
    }
}