package com.sep.vox.application.usecase.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.usecase.subscription.RecordAiUsageUseCase;
import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.model.metering.AiUsageType;
import com.sep.vox.domain.repository.AiUsageRecordRepository;

/**
 * usageEventId là khoá idempotency do Agentic AI sinh cho từng usage-event -- Kafka có thể redeliver
 * cùng message khi retry/rebalance, nên use case phải bỏ qua thay vì ghi trùng (xem
 * uk_ai_usage_record_usage_event_id ở V21__ai_usage_record.sql).
 */
class RecordAiUsageUseCaseTests {

    private AiUsageRecordRepository aiUsageRecordRepository;
    private RecordAiUsageUseCase useCase;

    private final UUID examSessionId = UUID.randomUUID();
    private final UUID turnId = UUID.randomUUID();
    private final UUID usageEventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        aiUsageRecordRepository = mock(AiUsageRecordRepository.class);
        useCase = new RecordAiUsageUseCase(aiUsageRecordRepository);
    }

    private RecordAiUsageCommand llmCommand() {
        return new RecordAiUsageCommand(
            examSessionId, turnId, usageEventId, AiUsageType.LLM_TOKEN,
            "anthropic", "claude-opus-4-8", 1400, 320, 0, 900,
            null, "{\"inputPerMtok\":5.00}", new BigDecimal("0.0132"), Instant.now()
        );
    }

    @Test
    void savesNewUsageRecordWhenNotSeenBefore() {
        when(aiUsageRecordRepository.existsByUsageEventId(usageEventId)).thenReturn(false);

        useCase.execute(llmCommand());

        verify(aiUsageRecordRepository).save(any(AiUsageRecord.class));
    }

    @Test
    void skipsSavingWhenUsageEventIdAlreadyRecorded() {
        when(aiUsageRecordRepository.existsByUsageEventId(usageEventId)).thenReturn(true);

        useCase.execute(llmCommand());

        verify(aiUsageRecordRepository, never()).save(any());
    }

    @Test
    void rejectsMissingUsageEventId() {
        var command = new RecordAiUsageCommand(
            examSessionId, turnId, null, AiUsageType.DURATION,
            "azure_stt", null, null, null, null, null,
            4200L, "{\"amount\":0.006}", new BigDecimal("0.0252"), Instant.now()
        );

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class);

        verify(aiUsageRecordRepository, never()).save(any());
    }

    @Test
    void rejectsMissingExamSessionId() {
        var command = new RecordAiUsageCommand(
            null, turnId, usageEventId, AiUsageType.LLM_TOKEN,
            "anthropic", "claude-opus-4-8", 100, 50, 0, 0,
            null, "{}", new BigDecimal("0.01"), Instant.now()
        );

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class);

        verify(aiUsageRecordRepository, never()).save(any());
    }
}