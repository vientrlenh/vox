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
    List<SchoolSubscriptionQuotaRecord> findBySchoolSubscriptionId(UUID schoolSubscriptionId);
    Optional<SchoolSubscriptionQuotaRecord> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, QuotaType quotaType);
    void addAllocation(UUID quotaId, BigDecimal amount);

    /** returns false if usedAmountVnd + amount would exceed totalAllocatedAmountVnd. */
    boolean tryConsume(UUID quotaId, BigDecimal amount);

    /** Unconditional -- always succeeds, can push usedAmountVnd above totalAllocatedAmountVnd (debt). */
    void addUsage(UUID quotaId, BigDecimal amount);
}
