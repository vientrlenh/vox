package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.AiUsageRecord;

public interface AiUsageRecordRepository {
    Optional<AiUsageRecord> findById(UUID id);
    AiUsageRecord save(AiUsageRecord record);
    List<AiUsageRecord> findByExamSessionId(UUID examSessionId);
    boolean existsByUsageEventId(UUID usageEventId);

    /**
     * Tổng cost_usd của mọi usage record thuộc session -- giữ để ghi vào
     * school_balance_entries.cost_usd (đối soát ngược với hóa đơn nhà cung cấp), KHÔNG dùng để trừ
     * hạn mức nữa: hạn mức tính bằng VND, xem {@link #sumCostVndByExamSessionId}.
     */
    BigDecimal sumCostUsdByExamSessionId(UUID examSessionId);

    /**
     * Tổng cost_vnd của mọi usage record thuộc session -- nguồn thật để trừ
     * SchoolSubscriptionQuotaRecord.
     *
     * <p>Cộng cost_vnd đã chốt sẵn từng dòng chứ KHÔNG quy đổi tổng USD theo tỷ giá hôm nay: mỗi
     * dòng đã ghi tỷ giá đúng lúc chi phí phát sinh (fx_rate_used), nên một phiên thi vắt qua ngày
     * đổi tỷ giá vẫn ra đúng số tiền thật, và trừ lại lần nữa cũng không cho ra con số khác.
     */
    BigDecimal sumCostVndByExamSessionId(UUID examSessionId);

    /**
     * Tổng cost_usd theo TỪNG session có occurred_at >= since -- dùng cho
     * QuotaPricingCalibrationService, không phải để trừ quota (khác sumCostUsdByExamSessionId ở
     * trên). Chỉ aggregate 1 bảng (không join) nên an toàn, không bị cartesian product.
     */
    List<SessionCostAggregate> sumCostUsdGroupedBySessionSince(Instant since);
}