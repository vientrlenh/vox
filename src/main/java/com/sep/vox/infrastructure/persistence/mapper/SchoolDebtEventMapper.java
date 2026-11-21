package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolDebtEvent;
import com.sep.vox.domain.model.school.SchoolDebtEventType;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolDebtEventJpaEntity;

public final class SchoolDebtEventMapper {

    private SchoolDebtEventMapper() {}

    public static SchoolDebtEvent toDomain(SchoolDebtEventJpaEntity jpa) {
        return SchoolDebtEvent.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .subscriptionId(jpa.getSubscriptionId())
            .eventType(SchoolDebtEventType.valueOf(jpa.getEventType()))
            // null is valid: a CLEARED row is not tied to any quota wallet.
            .quotaType(jpa.getQuotaType() == null ? null : QuotaType.valueOf(jpa.getQuotaType()))
            .triggerAssessmentSessionId(jpa.getTriggerAssessmentSessionId())
            .triggerPracticeSessionId(jpa.getTriggerPracticeSessionId())
            .triggerAmountVnd(jpa.getTriggerAmountVnd())
            .totalAllocatedVnd(jpa.getTotalAllocatedVnd())
            .usedAmountVnd(jpa.getUsedAmountVnd())
            .overageVnd(jpa.getOverageVnd())
            .occurredAt(jpa.getOccurredAt())
            .build();
    }

    public static SchoolDebtEventJpaEntity toJpa(SchoolDebtEvent domain) {
        return SchoolDebtEventJpaEntity.builder()
            .id(domain.getId())
            .schoolId(domain.getSchoolId())
            .subscriptionId(domain.getSubscriptionId())
            .eventType(domain.getEventType().name())
            .quotaType(domain.getQuotaType() == null ? null : domain.getQuotaType().name())
            .triggerAssessmentSessionId(domain.getTriggerAssessmentSessionId())
            .triggerPracticeSessionId(domain.getTriggerPracticeSessionId())
            .triggerAmountVnd(domain.getTriggerAmountVnd())
            .totalAllocatedVnd(domain.getTotalAllocatedVnd())
            .usedAmountVnd(domain.getUsedAmountVnd())
            .overageVnd(domain.getOverageVnd())
            .occurredAt(domain.getOccurredAt())
            .build();
    }
}
