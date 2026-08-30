package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolDebtEvent;
import com.sep.vox.domain.model.school.SchoolDebtEventType;
import com.sep.vox.infrastructure.persistence.entity.SchoolDebtEventJpaEntity;

public final class SchoolDebtEventMapper {

    private SchoolDebtEventMapper() {}

    public static SchoolDebtEvent toDomain(SchoolDebtEventJpaEntity jpa) {
        return new SchoolDebtEvent(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getSubscriptionId(),
            SchoolDebtEventType.valueOf(jpa.getEventType()),
            // null hợp lệ: dòng CLEARED không gắn với ví hạn mức nào.
            jpa.getQuotaType() == null ? null : QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getTriggerExamSessionId(),
            jpa.getTriggerPracticeSessionId(),
            jpa.getTriggerAmountVnd(),
            jpa.getTotalAllocatedVnd(),
            jpa.getUsedAmountVnd(),
            jpa.getOverageVnd(),
            jpa.getOccurredAt()
        );
    }

    public static SchoolDebtEventJpaEntity toJpa(SchoolDebtEvent domain) {
        return new SchoolDebtEventJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionId(),
            domain.getEventType().name(),
            domain.getQuotaType() == null ? null : domain.getQuotaType().name(),
            domain.getTriggerExamSessionId(),
            domain.getTriggerPracticeSessionId(),
            domain.getTriggerAmountVnd(),
            domain.getTotalAllocatedVnd(),
            domain.getUsedAmountVnd(),
            domain.getOverageVnd(),
            domain.getOccurredAt()
        );
    }
}
