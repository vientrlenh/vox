package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;

/**
 * Một bút toán trên sổ cái ví, dạng đọc. Ánh xạ 1-1 với {@link SchoolBalanceEntry} -- không join,
 * không làm giàu dữ liệu.
 *
 * <p>Trả {@code actorId} thô chứ không phải tên người thực hiện: cùng cách {@code SchoolDebtEvent}
 * trả {@code triggerExamSessionId}. Muốn hiện tên thì cần LEFT JOIN sang users, và khi đó DTO không
 * còn dựng được từ một mình domain model nữa -- đó là một quyết định riêng, đáng làm khi có màn hình
 * thật sự cần, không phải làm sẵn ở đây.
 *
 * <p>Bốn cột tiền là String -- xem {@link DecimalText} và khối chú thích đầu school-balance.graphqls.
 */
public record SchoolBalanceEntryDto(
    UUID id,
    UUID schoolId,
    String entryType,
    String amountVnd,
    String balanceAfterVnd,
    String occurredAt,
    UUID subscriptionId,
    UUID orderId,
    UUID examSessionId,
    UUID practiceSessionId,
    String quotaType,
    String costUsd,
    String fxRateUsed,
    String reason,
    UUID actorId
) {

    public static SchoolBalanceEntryDto toDto(SchoolBalanceEntry entry) {
        return new SchoolBalanceEntryDto(
            entry.getId(),
            entry.getSchoolId(),
            entry.getEntryType() == null ? null : entry.getEntryType().name(),
            DecimalText.of(entry.getAmountVnd()),
            DecimalText.of(entry.getBalanceAfterVnd()),
            entry.getOccurredAt() == null ? null : entry.getOccurredAt().toString(),
            entry.getSubscriptionId(),
            entry.getOrderId(),
            entry.getExamSessionId(),
            entry.getPracticeSessionId(),
            // Nullable ở tầng DB: chỉ OVERAGE_CHARGE mới bắt buộc có quota_type.
            entry.getQuotaType() == null ? null : entry.getQuotaType().name(),
            DecimalText.of(entry.getCostUsd()),
            DecimalText.of(entry.getFxRateUsed()),
            entry.getReason(),
            entry.getActorId()
        );
    }
}
