package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

public interface SchoolSubscriptionQuotaRecordRepository {
    Optional<SchoolSubscriptionQuotaRecord> findById(UUID id);
    SchoolSubscriptionQuotaRecord save(SchoolSubscriptionQuotaRecord quota);
    List<SchoolSubscriptionQuotaRecord> findAllBySubscriptionId(UUID subscriptionId);
    Optional<SchoolSubscriptionQuotaRecord> findBySubscriptionIdAndQuotaType(UUID subscriptionId, QuotaType quotaType);
    void addAllocation(UUID quotaId, BigDecimal amount);

    /** returns false if usedQuantity + amount would exceed totalAllocated. */
    boolean tryConsume(UUID quotaId, BigDecimal amount);

    /** Unconditional -- always succeeds, can push usedQuantity above totalAllocated (debt). */
    void addUsage(UUID quotaId, BigDecimal amount);
}