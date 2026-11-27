package com.sep.vox.domain.model.metering;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiUsageRecordTests {

    @Test
    void recordedSetsEveryArgument() {
        UUID sessionId = UUID.randomUUID();
        UUID turnId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-11-25T01:00:00Z");

        AiUsageRecord record = AiUsageRecord.recorded(
                sessionId, turnId, eventId, AiUsageType.LLM_TOKEN, "azure", "gpt", 10, 20, 3, 4, 500L, "{}",
                new BigDecimal("0.01"), new BigDecimal("250"), new BigDecimal("25000"), occurredAt);

        assertThat(record.getId()).isNull();
        assertThat(record.getAssessmentSessionId()).isEqualTo(sessionId);
        assertThat(record.getTurnId()).isEqualTo(turnId);
        assertThat(record.getUsageEventId()).isEqualTo(eventId);
        assertThat(record.getUsageType()).isEqualTo(AiUsageType.LLM_TOKEN);
        assertThat(record.getProvider()).isEqualTo("azure");
        assertThat(record.getModelName()).isEqualTo("gpt");
        assertThat(record.getInputTokens()).isEqualTo(10);
        assertThat(record.getOutputTokens()).isEqualTo(20);
        assertThat(record.getCacheCreationInputTokens()).isEqualTo(3);
        assertThat(record.getCacheReadInputTokens()).isEqualTo(4);
        assertThat(record.getDurationMs()).isEqualTo(500L);
        assertThat(record.getUnitPriceJson()).isEqualTo("{}");
        assertThat(record.getCostUsd()).isEqualByComparingTo("0.01");
        assertThat(record.getCostVnd()).isEqualByComparingTo("250");
        assertThat(record.getFxRateUsed()).isEqualByComparingTo("25000");
        assertThat(record.getOccurredAt()).isEqualTo(occurredAt);
    }
}
