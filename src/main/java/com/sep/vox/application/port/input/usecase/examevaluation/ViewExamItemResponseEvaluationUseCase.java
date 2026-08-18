package com.sep.vox.application.port.input.usecase.examevaluation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewExamItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.examitemresponse.ExamItemAiEvaluationContextResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemCriterionScoreResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationTurnResponse;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
public class ViewExamItemResponseEvaluationUseCase
        implements IUseCase<ViewExamItemResponseEvaluationQuery, ExamItemEvaluationDetailsResponse> {

    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ExamResultAccessService examResultAccessService;
    private final JsonSerializationPort jsonSerializationPort;

    public ViewExamItemResponseEvaluationUseCase(
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ExamResultAccessService examResultAccessService,
            JsonSerializationPort jsonSerializationPort) {
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examItemEvaluationTurnRepository = examItemEvaluationTurnRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.examResultAccessService = examResultAccessService;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ExamItemEvaluationDetailsResponse execute(ViewExamItemResponseEvaluationQuery input) {
        var access = examResultAccessService.requireCandidateVisibleResponse(input.answerId());
        var evaluation = examItemEvaluationRepository.findLatestByResponseId(input.answerId())
            .orElse(null);
        // Chưa ai chấm câu này -- vẫn phải trả về bản ghi bài nói.
        //
        // Trước đây chỗ này orElseThrow(NotFoundException). Bài bị buộc kết thúc rồi gỡ vi phạm
        // chưa từng có evaluation nào, nên trang Xem kết quả không lấy được audio lẫn transcript
        // dù exam_item_response_turns còn nguyên -- trong khi màn chấm hiện đủ, vì nó LEFT JOIN
        // sang evaluation thay vì đòi phải có. Hai trang đọc cùng một dữ liệu bằng hai giả định
        // trái ngược nhau; đây là kéo trang kết quả về giả định của màn chấm.
        //
        // Điểm và tiêu chí để trống vì thật sự chưa có. Bản ghi tiếng nói thì không phải điểm --
        // nó là bằng chứng, và không có lý do gì bắt chờ chấm xong mới cho xem.
        if (evaluation == null) {
            return ungradedDetails(access.response());
        }

        // Bản hiệu lực là bản chấm tay ⇒ mọi bằng chứng AI nằm ở bản AI đã bị SUPERSEDED,
        // phải đi tìm riêng. Bản hiệu lực đã là AI thì chính nó — không tốn thêm query.
        var aiEvaluation = evaluation.getEngineType() == ExamEvaluationEngineType.HUMAN
            ? examItemEvaluationRepository.findLatestAiByResponseId(input.answerId()).orElse(null)
            : evaluation;

        // criteria vẫn theo bản hiệu lực: sau khi chấm lại thì đây là điểm và rationale của
        // GIÁO VIÊN, đúng như thiết kế. Chỉ ngữ cảnh mới lấy từ bản AI.
        var criteria = examItemCriterionScoreRepository.findByEvaluationId(evaluation.getId()).stream()
            .map(item -> {
                var criterion = rubricCriterionRepository.findById(item.getRubricCriterionId()).orElse(null);
                return new ExamItemCriterionScoreResponse(
                    item.getId(),
                    item.getRubricCriterionId(),
                    criterion == null ? null : criterion.getCode(),
                    criterion == null ? null : criterion.getName(),
                    criterion == null ? null : criterion.getMinScore(),
                    criterion == null ? null : criterion.getMaxScore(),
                    item.getRawScore(),
                    item.getFinalScore(),
                    item.getRationale()
                );
            })
            .toList();
        // Turn chỉ tồn tại ở bản AI. Đọc theo bản hiệu lực là mất audio/transcript/nội dung
        // câu hỏi ngay sau lần chấm lại đầu tiên — chính là lỗi đang sửa.
        var turnSourceId = (aiEvaluation == null ? evaluation : aiEvaluation).getId();
        List<ExamItemEvaluationTurnResponse> turns = examItemEvaluationTurnRepository.findByEvaluationId(turnSourceId).stream()
            .map(item -> new ExamItemEvaluationTurnResponse(
                item.getId(),
                item.getTurnOrder(),
                item.getTurnType().name(),
                item.getPromptText(),
                item.getAudioUrl(),
                item.getTranscript(),
                item.getWordCount(),
                item.getDurationSeconds(),
                item.getAsrConfidence(),
                item.getPronunciationOverallJson(),
                item.getWordFeedbackJson()
            ))
            .toList();
        // Không có bản AI nào -> lùi về lượt nói GỐC của thí sinh.
        //
        // Bài bị buộc kết thúc giữa giờ chưa từng được AI chấm, và bài giáo viên chấm tay chỉ
        // sinh bản HUMAN -- mà HUMAN không có turn nào. Cả hai đều rơi vào đây, và trang Xem
        // kết quả hiện câu hỏi trống trơn: không audio, không transcript, không đề bài, dù
        // exam_item_response_turns còn nguyên (đo được 2026-08-17, phiên 01a00e64).
        //
        // Ba trường riêng của bản AI (asrConfidence, pronunciationOverall, wordFeedback) để
        // null: bảng gốc không có chúng, và bịa giá trị còn tệ hơn để trống. Cùng cách xử lý
        // với JpaExamGradingQueryRepository#turnsForItem bên màn chấm.
        if (turns.isEmpty()) {
            turns = examItemResponseTurnRepository.findByExamItemResponseId(evaluation.getResponseId()).stream()
                .map(item -> new ExamItemEvaluationTurnResponse(
                    item.getId(),
                    item.getTurnOrder(),
                    item.getTurnType().name(),
                    item.getPromptText(),
                    item.getAudioUrl(),
                    item.getTranscript(),
                    item.getWordCount(),
                    item.getDurationSeconds(),
                    null,
                    null,
                    null
                ))
                .toList();
        }

        return new ExamItemEvaluationDetailsResponse(
            evaluation.getId(),
            evaluation.getResponseId(),
            evaluation.getPaperItemId(),
            evaluation.getEngineType().name(),
            evaluation.getGradedByModel(),
            evaluation.getPromptVersion(),
            evaluation.getRawItemScore(),
            evaluation.getItemScore(),
            evaluation.getOverallConfidence(),
            evaluation.isRequiresHumanReview(),
            evaluation.getReviewReasonCode(),
            evaluation.isMarkedInvalid(),
            evaluation.isRequiresRetake(),
            evaluation.getStatus().name(),
            evaluation.getEvaluatedAt() == null ? null : evaluation.getEvaluatedAt().toString(),
            evaluation.getFeedbackSummary(),
            jsonSerializationPort.toJson(evaluation.getSignals()),
            evaluation.getValidityJson(),
            evaluation.getSuggestionsJson(),
            criteria,
            turns,
            toAiContext(aiEvaluation)
        );
    }

    /**
     * Câu chưa có bản chấm nào: chỉ có bản ghi bài nói, không có điểm.
     *
     * <p>{@code id} null là tín hiệu để client biết "chưa chấm" mà không phải suy từ việc thiếu
     * điểm -- điểm 0 hợp lệ cũng thiếu điểm theo nghĩa đó. Ba trường riêng của bản AI
     * (asrConfidence, pronunciationOverall, wordFeedback) cũng null: bảng gốc không có chúng, và
     * bịa giá trị còn tệ hơn để trống.
     */
    private ExamItemEvaluationDetailsResponse ungradedDetails(
            com.sep.vox.domain.model.exam.ExamItemResponse response) {
        var turns = examItemResponseTurnRepository.findByExamItemResponseId(response.getId()).stream()
            .map(item -> new ExamItemEvaluationTurnResponse(
                item.getId(),
                item.getTurnOrder(),
                item.getTurnType().name(),
                item.getPromptText(),
                item.getAudioUrl(),
                item.getTranscript(),
                item.getWordCount(),
                item.getDurationSeconds(),
                null,
                null,
                null
            ))
            .toList();
        return new ExamItemEvaluationDetailsResponse(
            null,
            response.getId(),
            response.getPaperItemId(),
            null, null, null,
            null, null, null,
            false, null, false, false,
            null, null, null, null, null, null,
            List.of(),
            turns,
            null
        );
    }

    private ExamItemAiEvaluationContextResponse toAiContext(ExamItemEvaluation aiEvaluation) {
        if (aiEvaluation == null) {
            return null;
        }
        return new ExamItemAiEvaluationContextResponse(
            aiEvaluation.getId(),
            aiEvaluation.getEngineType().name(),
            aiEvaluation.getGradedByModel(),
            aiEvaluation.getPromptVersion(),
            aiEvaluation.getOverallConfidence(),
            aiEvaluation.isRequiresHumanReview(),
            aiEvaluation.getReviewReasonCode(),
            aiEvaluation.isMarkedInvalid(),
            aiEvaluation.isRequiresRetake(),
            aiEvaluation.getEvaluatedAt() == null ? null : aiEvaluation.getEvaluatedAt().toString(),
            aiEvaluation.getFeedbackSummary(),
            jsonSerializationPort.toJson(aiEvaluation.getSignals()),
            aiEvaluation.getValidityJson(),
            aiEvaluation.getSuggestionsJson()
        );
    }
}
