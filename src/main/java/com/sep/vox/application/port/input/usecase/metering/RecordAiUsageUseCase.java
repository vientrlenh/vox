package com.sep.vox.application.port.input.usecase.metering;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.repository.AiUsageRecordRepository;

// Internal service-to-service use case (a Kafka consumer receiving the usage report from the Agentic AI after every turn,
// and the REST path ReportAiUsageUseCase for sources that cannot reach Kafka), not end-user-facing.
@Service
public class RecordAiUsageUseCase implements IUseCase<RecordAiUsageCommand, Void> {

    // Trùng scale của ai_usage_records.cost_vnd numeric(18,6): rộng hơn thì Postgres cắt tiếp lúc ghi
    // (Java và DB nói hai số khác nhau), hẹp hơn thì mất tiền ngay từ đây.
    private static final int COST_VND_SCALE = 6;

    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final QuotaPricingPort quotaPricingPort;

    public RecordAiUsageUseCase(
            AiUsageRecordRepository aiUsageRecordRepository,
            QuotaPricingPort quotaPricingPort) {
        this.aiUsageRecordRepository = aiUsageRecordRepository;
        this.quotaPricingPort = quotaPricingPort;
    }

    @Override
    @Transactional
    public Void execute(RecordAiUsageCommand input) {
        if (input.usageEventId() == null) {
            throw new IllegalArgumentException("Thiếu usageEventId trong dữ liệu usage");
        }
        if (input.assessmentSessionId() == null) {
            throw new IllegalArgumentException("Thiếu assessmentSessionId trong dữ liệu usage");
        }
        // Block early instead of letting an NPE happen in the multiplication below: cost_vnd is NOT NULL, so a row missing
        // costUsd cannot be written, and swallowing it silently means losing a real charge entirely.
        if (input.costUsd() == null) {
            throw new IllegalArgumentException("Thiếu costUsd trong dữ liệu usage");
        }

        // Idempotent: Kafka may redeliver the same usageEventId on retry/rebalance -- skip instead of writing a duplicate;
        // uk_ai_usage_record_usage_event_id is the last guard if a rare race slips through here.
        if (aiUsageRecordRepository.existsByUsageEventId(input.usageEventId())) {
            return null;
        }

        // Go through QuotaPricingPort instead of reading ExchangeRateSnapshotRepository directly: that port already combines
        // "the latest exchange rate, falling back to the constant in .env when no fetch has succeeded yet", so repeating the logic
        // here would build a second exchange rate that silently drifts from the quota selling price.
        //
        // Fix the exchange rate AT WRITE TIME and store it on this very row: a Kafka/DLT retry may push an event to the next day,
        // and converting again with the "latest" rate would then give a different number from the one at the time the real cost
        // arose. The idempotency constraint above guarantees an event is only fixed exactly once.
        BigDecimal exchangeRateVnd = quotaPricingPort.usdToVndRate();
        BigDecimal costVnd = input.costUsd().multiply(exchangeRateVnd).setScale(COST_VND_SCALE, RoundingMode.HALF_UP);

        aiUsageRecordRepository.save(AiUsageRecord.builder()
            .assessmentSessionId(input.assessmentSessionId())
            .turnId(input.turnId())
            .usageEventId(input.usageEventId())
            .usageType(input.usageType())
            .provider(input.provider())
            .modelName(input.modelName())
            .inputTokens(input.inputTokens())
            .outputTokens(input.outputTokens())
            .cacheCreationInputTokens(input.cacheCreationInputTokens())
            .cacheReadInputTokens(input.cacheReadInputTokens())
            .durationMs(input.durationMs())
            .unitPriceJson(input.unitPriceJson())
            .costUsd(input.costUsd())
            .costVnd(costVnd)
            .fxRateUsed(exchangeRateVnd)
            .occurredAt(input.occurredAt())
            .build());
        return null;
    }
}