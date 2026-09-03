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

    /**
     * Chuyển tiền từ ví tự nạp vào ví hạn mức: cộng vào cả {@code totalAllocatedAmountVnd} lẫn
     * {@code fundedFromBalanceVnd} trong cùng một câu lệnh. Chỗ gọi PHẢI đã trừ ví và ghi bút toán
     * QUOTA_FUNDING trong cùng transaction -- xem FundQuotaFromBalanceUseCase.
     */
    void addFundingFromBalance(UUID quotaId, BigDecimal amount);

    /** returns false if usedAmountVnd + amount would exceed totalAllocatedAmountVnd. */
    boolean tryConsume(UUID quotaId, BigDecimal amount);

    /**
     * Unconditional -- always succeeds. Chỉ dùng để tiêu NỐT phần hạn mức còn lại cho đầy khi một
     * khoản chi vắt qua trần (ConsumeQuotaService.chargeOverage); chỗ gọi tự kẹp amount về đúng phần
     * còn lại. KHÔNG dùng để ghi nợ nữa: nợ giờ là balance_vnd âm, và usedAmountVnd vượt
     * totalAllocatedAmountVnd là dữ liệu hỏng chứ không còn là một trạng thái hợp lệ.
     */
    void addUsage(UUID quotaId, BigDecimal amount);
}
