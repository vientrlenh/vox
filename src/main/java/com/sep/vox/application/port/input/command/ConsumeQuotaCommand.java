package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.QuotaType;

public record ConsumeQuotaCommand(
    UUID subscriptionId,
    UUID examSessionId,
    QuotaType quotaType,
    BigDecimal amount,
    UUID userId,
    // true = chi phí AI thật đã phát sinh, PHẢI ghi nhận đủ dù vượt hạn mức (cho phép âm/ghi nợ) --
    // dùng cho deduction sau khi chấm bài (CompleteExamSessionGradingUseCase). false (mặc định các
    // nơi khác) = giữ hành vi chặn cứng cũ (QuotaExceededException), dùng cho PRACTICE (pre-flight
    // per-turn check) và consume thủ công qua API nội bộ.
    boolean allowDebt
) {
    public ConsumeQuotaCommand(UUID subscriptionId, UUID examSessionId, QuotaType quotaType, BigDecimal amount, UUID userId) {
        this(subscriptionId, examSessionId, quotaType, amount, userId, false);
    }
}