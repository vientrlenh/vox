package com.sep.vox.domain.model.school;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.model.metering.QuotaType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchoolDebtEventTests {

    @Test
    void recordedSetsEveryArgument() {
        UUID schoolId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID assessmentSessionId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-11-25T01:00:00Z");

        SchoolDebtEvent event = SchoolDebtEvent.recorded(
                schoolId, subscriptionId, SchoolDebtEventType.LOCKED, QuotaType.ASSESSMENT, assessmentSessionId, null,
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("10"), occurredAt);

        assertThat(event.getId()).isNull();
        assertThat(event.getSchoolId()).isEqualTo(schoolId);
        assertThat(event.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(event.getEventType()).isEqualTo(SchoolDebtEventType.LOCKED);
        assertThat(event.getQuotaType()).isEqualTo(QuotaType.ASSESSMENT);
        assertThat(event.getTriggerAssessmentSessionId()).isEqualTo(assessmentSessionId);
        assertThat(event.getTriggerPracticeSessionId()).isNull();
        assertThat(event.getTriggerAmountVnd()).isEqualByComparingTo("10");
        assertThat(event.getTotalAllocatedVnd()).isEqualByComparingTo("100");
        assertThat(event.getUsedAmountVnd()).isEqualByComparingTo("110");
        assertThat(event.getOverageVnd()).isEqualByComparingTo("10");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }
}
