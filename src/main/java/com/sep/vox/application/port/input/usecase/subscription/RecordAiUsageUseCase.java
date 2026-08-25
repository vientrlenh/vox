package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.aimodel.AiUsageRecord;
import com.sep.vox.domain.repository.AiUsageRecordRepository;

// Internal service-to-service use case (Kafka consumer nhận usage report từ Agentic AI sau mỗi turn),
// không end-user-facing.
@Service
public class RecordAiUsageUseCase implements IUseCase<RecordAiUsageCommand, Void> {

    private final AiUsageRecordRepository aiUsageRecordRepository;

    public RecordAiUsageUseCase(AiUsageRecordRepository aiUsageRecordRepository) {
        this.aiUsageRecordRepository = aiUsageRecordRepository;
    }

    @Override
    @Transactional
    public Void execute(RecordAiUsageCommand input) {
        if (input.usageEventId() == null) {
            throw new IllegalArgumentException("Thiếu usageEventId trong dữ liệu usage");
        }
        if (input.examSessionId() == null) {
            throw new IllegalArgumentException("Thiếu examSessionId trong dữ liệu usage");
        }

        // Idempotent: Kafka có thể redeliver cùng usageEventId khi retry/rebalance -- bỏ qua thay vì
        // ghi trùng, uk_ai_usage_record_usage_event_id là chốt chặn cuối nếu race hiếm gặp lọt qua đây.
        if (aiUsageRecordRepository.existsByUsageEventId(input.usageEventId())) {
            return null;
        }

        aiUsageRecordRepository.save(new AiUsageRecord(
            input.examSessionId(),
            input.turnId(),
            input.usageEventId(),
            input.usageType(),
            input.provider(),
            input.modelName(),
            input.inputTokens(),
            input.outputTokens(),
            input.cacheCreationInputTokens(),
            input.cacheReadInputTokens(),
            input.durationMs(),
            input.unitPriceJson(),
            input.costUsd(),
            input.occurredAt()
        ));
        return null;
    }
}