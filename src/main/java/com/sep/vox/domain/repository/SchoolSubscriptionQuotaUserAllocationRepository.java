package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;

public interface SchoolSubscriptionQuotaUserAllocationRepository {
    List<SchoolSubscriptionQuotaUserAllocation> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, QuotaType quotaType);
    Optional<SchoolSubscriptionQuotaUserAllocation> findBySchoolSubscriptionIdAndQuotaTypeAndUserId(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId);

    /** Set-based upsert: creates the row if missing, otherwise overwrites allocatedAmountVnd. */
    SchoolSubscriptionQuotaUserAllocation upsertAllocation(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId, BigDecimal allocatedAmountVnd);

    /** returns false if usedAmountVnd + amount would exceed allocatedAmountVnd. */
    boolean tryConsume(UUID id, BigDecimal amount);

    /**
     * Tổng phần đã hứa cho từng người mà họ CHƯA tiêu, kẹp về 0 theo TỪNG DÒNG. Không bao giờ null --
     * trường chưa chia cho ai trả về 0.
     *
     * <p>Đây là con số duy nhất đem trừ khỏi ví trường được mà không trừ trùng. Cộng thẳng
     * {@code allocatedAmountVnd} là sai: phần người ta đã tiêu đã nằm trong {@code used_amount_vnd}
     * của ví rồi -- xem ConsumeQuotaService.consumeUserAllocation.
     */
    BigDecimal sumUnusedAllocation(UUID schoolSubscriptionId, QuotaType quotaType);

    /** Unconditional -- always succeeds, can push usedAmountVnd above allocatedAmountVnd (debt). */
    void addUsage(UUID id, BigDecimal amount);
}
