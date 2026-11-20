package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolDebtEvent;

/**
 * Một dòng nhật ký nợ hạn mức, dạng đọc. Ánh xạ 1-1 với {@link SchoolDebtEvent}.
 *
 * <p>Bốn cột tiền là String chứ không phải BigDecimal/Float -- xem {@link DecimalText} và khối chú
 * thích đầu {@code school-balance.graphqls}. Chúng là {@code numeric(18,6)} giống sổ cái ví, nên phải
 * nói cùng một thứ tiếng với nó: cùng một khoản nợ xuất hiện ở cả hai sổ, và hai sổ đối soát với nhau
 * thì không được phép lệch vì làm tròn nhị phân.
 */
public record SchoolDebtEventDto(
    UUID id,
    UUID schoolId,
    UUID subscriptionId,
    String eventType,
    String quotaType,
    UUID triggerExamSessionId,
    UUID triggerPracticeSessionId,
    String triggerAmountVnd,
    String totalAllocatedVnd,
    String usedAmountVnd,
    String overageVnd,
    String occurredAt
) {

    public static SchoolDebtEventDto toDto(SchoolDebtEvent event) {
        return new SchoolDebtEventDto(
            event.getId(),
            event.getSchoolId(),
            event.getSubscriptionId(),
            event.getEventType() == null ? null : event.getEventType().name(),
            event.getQuotaType() == null ? null : event.getQuotaType().name(),
            event.getTriggerExamSessionId(),
            event.getTriggerPracticeSessionId(),
            // null với dòng CLEARED: hết nợ không do một khoản trừ nào gây ra.
            DecimalText.of(event.getTriggerAmountVnd()),
            DecimalText.of(event.getTotalAllocatedVnd()),
            DecimalText.of(event.getUsedAmountVnd()),
            DecimalText.of(event.getOverageVnd()),
            event.getOccurredAt() == null ? null : event.getOccurredAt().toString()
        );
    }
}
