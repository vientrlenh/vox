package com.sep.vox.application.port.input.usecase.question;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.QuestionAssetRepository;

@Service
public class RecordQuestionAssetAnalysisResultUseCase
        implements IUseCase<RecordQuestionAssetAnalysisResultUseCase.Command, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordQuestionAssetAnalysisResultUseCase.class);

    private final QuestionAssetRepository questionAssetRepository;

    public RecordQuestionAssetAnalysisResultUseCase(QuestionAssetRepository questionAssetRepository) {
        this.questionAssetRepository = questionAssetRepository;
    }

    @Override
    @Transactional
    public Void execute(Command input) {
        var asset = questionAssetRepository.findById(input.assetId()).orElse(null);
        if (asset == null) {
            // Race hợp lệ: asset có thể đã bị xoá hoặc question đã bị đổi (clone sang draft khác)
            // giữa lúc publish yêu cầu và lúc Python trả kết quả về -- không phải lỗi đáng retry/DLT.
            LOGGER.warn("Bỏ qua kết quả phân tích asset -- không còn tìm thấy assetId={}", input.assetId());
            return null;
        }

        // Chỉ điền vào field ĐANG rỗng -- nội dung người đã gõ tay hoặc AI đã viết trước đó luôn
        // thắng, không bao giờ bị ghi đè ở đây. "Tạo lại bằng AI" (RegenerateQuestionAssetAnalysisUseCase
        // .resetForAnalysis()) xoá trắng field trước khi publish chính là để luật này cho phép ghi lại.
        var changed = false;
        if (isBlank(asset.getTranscript()) && !isBlank(input.transcript())) {
            asset.setTranscript(input.transcript());
            changed = true;
        }
        if (isBlank(asset.getDescription()) && !isBlank(input.description())) {
            asset.setDescription(input.description());
            changed = true;
        }

        if (changed) {
            questionAssetRepository.save(asset);
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Command(UUID assetId, String transcript, String description) {
    }
}
