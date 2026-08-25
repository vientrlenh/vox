package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.aimodel.AiUsageRecord;

public interface AiUsageRecordRepository {
    Optional<AiUsageRecord> findById(UUID id);
    AiUsageRecord save(AiUsageRecord record);
    List<AiUsageRecord> findAllByExamSessionId(UUID examSessionId);
    boolean existsByUsageEventId(UUID usageEventId);

    /** Tổng cost_usd của mọi usage record thuộc session -- nguồn thật để trừ SubscriptionQuota. */
    BigDecimal sumCostUsdByExamSessionId(UUID examSessionId);

    /**
     * Tổng cost_usd theo TỪNG session có occurred_at >= since -- dùng cho
     * QuotaPricingCalibrationService, không phải để trừ quota (khác sumCostUsdByExamSessionId ở
     * trên). Chỉ aggregate 1 bảng (không join) nên an toàn, không bị cartesian product.
     */
    List<SessionCostAggregate> sumCostUsdGroupedBySessionSince(Instant since);
}