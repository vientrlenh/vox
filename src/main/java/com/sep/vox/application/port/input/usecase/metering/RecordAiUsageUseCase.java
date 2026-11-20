package com.sep.vox.application.port.input.usecase.metering;

import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.repository.AiUsageRecordRepository;

// Internal service-to-service use case (Kafka consumer nhận usage report từ Agentic AI sau mỗi turn,
// và đường REST ReportAiUsageUseCase cho nguồn không nối được Kafka), không end-user-facing.
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
        if (input.examSessionId() == null) {
            throw new IllegalArgumentException("Thiếu examSessionId trong dữ liệu usage");
        }
        // Chặn sớm thay vì để NPE ở phép nhân bên dưới: cost_vnd là NOT NULL nên một dòng thiếu
        // costUsd không thể ghi được, và nuốt lặng nó nghĩa là mất trắng một khoản chi thật.
        if (input.costUsd() == null) {
            throw new IllegalArgumentException("Thiếu costUsd trong dữ liệu usage");
        }

        // Idempotent: Kafka có thể redeliver cùng usageEventId khi retry/rebalance -- bỏ qua thay vì
        // ghi trùng, uk_ai_usage_record_usage_event_id là chốt chặn cuối nếu race hiếm gặp lọt qua đây.
        if (aiUsageRecordRepository.existsByUsageEventId(input.usageEventId())) {
            return null;
        }

        // Qua QuotaPricingPort chứ không tự đọc ExchangeRateSnapshotRepository: cổng đó đã gộp sẵn
        // "tỷ giá mới nhất, fallback về hằng số trong .env khi chưa có lần fetch nào thành công", nên
        // tự lặp lại logic ở đây là dựng một tỷ giá thứ hai âm thầm lệch khỏi giá bán quota.
        //
        // Chốt tỷ giá NGAY LÚC GHI và lưu vào chính dòng này: retry Kafka/DLT có thể đẩy một sự kiện
        // sang tận ngày hôm sau, lúc đó quy đổi lại theo tỷ giá "mới nhất" sẽ ra một con số khác với
        // lúc chi phí thật phát sinh. Ràng buộc idempotency ở trên bảo đảm một sự kiện chỉ được chốt
        // đúng một lần.
        var exchangeRateVnd = quotaPricingPort.usdToVndRate();
        var costVnd = input.costUsd().multiply(exchangeRateVnd).setScale(COST_VND_SCALE, RoundingMode.HALF_UP);

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
            costVnd, 
            exchangeRateVnd,
            input.occurredAt()
        ));
        return null;
    }
}