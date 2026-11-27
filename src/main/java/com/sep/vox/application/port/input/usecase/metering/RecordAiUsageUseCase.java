package com.sep.vox.application.port.input.usecase.metering;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.metering.RecordAiUsageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.repository.AiUsageRecordRepository;

// Internal service-to-service use case (a Kafka consumer receiving the usage report from the Agentic AI after every turn,
// and the REST path ReportAiUsageUseCase for sources that cannot reach Kafka), not end-user-facing.
@Service
@RequiredArgsConstructor
public class RecordAiUsageUseCase implements IUseCase<RecordAiUsageCommand, Void> {

    // Matches the scale of ai_usage_records.cost_vnd numeric(18,6): if wider, Postgres truncates further on write
    // (Java and the DB report two different numbers); if narrower, money is lost right here.
    private static final int COST_VND_SCALE = 6;

    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final QuotaPricingPort quotaPricingPort;

    @Override
    @Transactional
    public Void execute(RecordAiUsageCommand input) {
        requireRequiredUsageFields(input);

        // Idempotent: Kafka may redeliver the same usageEventId on retry/rebalance -- skip instead of writing a duplicate;
        // uk_ai_usage_record_usage_event_id is the last guard if a rare race slips through here.
        if (aiUsageRecordRepository.existsByUsageEventId(input.usageEventId())) {
            return null;
        }

        saveUsageRecord(input);
        return null;
    }

    private void requireRequiredUsageFields(RecordAiUsageCommand input) {
        if (input.usageEventId() == null) {
            throw new IllegalArgumentException("usageEventId is missing from the usage data");
        }
        if (input.assessmentSessionId() == null) {
            throw new IllegalArgumentException("assessmentSessionId is missing from the usage data");
        }
        // Block early instead of letting an NPE happen in the multiplication in saveUsageRecord: cost_vnd is NOT NULL, so a row missing
        // costUsd cannot be written, and swallowing it silently means losing a real charge entirely.
        if (input.costUsd() == null) {
            throw new IllegalArgumentException("costUsd is missing from the usage data");
        }
    }

    private void saveUsageRecord(RecordAiUsageCommand input) {
        // Go through QuotaPricingPort instead of reading ExchangeRateSnapshotRepository directly: that port already combines
        // "the latest exchange rate, falling back to the constant in .env when no fetch has succeeded yet", so repeating the logic
        // here would build a second exchange rate that silently drifts from the quota selling price.
        //
        // Fix the exchange rate AT WRITE TIME and store it on this very row: a Kafka/DLT retry may push an event to the next day,
        // and converting again with the "latest" rate would then give a different number from the one at the time the real cost
        // arose. The idempotency constraint in execute guarantees an event is only fixed exactly once.
        BigDecimal exchangeRateVnd = quotaPricingPort.usdToVndRate();
        BigDecimal costVnd = input.costUsd().multiply(exchangeRateVnd).setScale(COST_VND_SCALE, RoundingMode.HALF_UP);

        aiUsageRecordRepository.save(AiUsageRecord.recorded(
            input.assessmentSessionId(), input.turnId(), input.usageEventId(), input.usageType(), input.provider(),
            input.modelName(), input.inputTokens(), input.outputTokens(), input.cacheCreationInputTokens(),
            input.cacheReadInputTokens(), input.durationMs(), input.unitPriceJson(), input.costUsd(), costVnd,
            exchangeRateVnd, input.occurredAt()));
    }
}