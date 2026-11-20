package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import java.util.List;

import java.util.UUID;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAiEvaluationContextResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemCriterionScoreResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationTurnResponse;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
public class ViewAssessmentItemResponseEvaluationUseCase
        implements IUseCase<ViewAssessmentItemResponseEvaluationQuery, AssessmentItemEvaluationDetailsResponse> {

    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private final AssessmentItemEvaluationTurnRepository assessmentItemEvaluationTurnRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final AssessmentResultAccessService assessmentResultAccessService;
    private final JsonSerializationPort jsonSerializationPort;

    public ViewAssessmentItemResponseEvaluationUseCase(
            AssessmentItemEvaluationRepository assessmentItemEvaluationRepository,
            AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository,
            AssessmentItemEvaluationTurnRepository assessmentItemEvaluationTurnRepository,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository,
            RubricCriterionRepository rubricCriterionRepository,
            AssessmentResultAccessService assessmentResultAccessService,
            JsonSerializationPort jsonSerializationPort) {
        this.assessmentItemEvaluationRepository = assessmentItemEvaluationRepository;
        this.assessmentItemCriterionScoreRepository = assessmentItemCriterionScoreRepository;
        this.assessmentItemEvaluationTurnRepository = assessmentItemEvaluationTurnRepository;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.assessmentResultAccessService = assessmentResultAccessService;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public AssessmentItemEvaluationDetailsResponse execute(ViewAssessmentItemResponseEvaluationQuery input) {
        AssessmentResultAccessService.ResponseAccess access = assessmentResultAccessService.requireCandidateVisibleResponse(input.answerId());
        AssessmentItemEvaluation evaluation = assessmentItemEvaluationRepository.findLatestByResponseId(input.answerId())
            .orElse(null);
        // Nobody has graded this question yet -- the speech recording must still be returned.
        //
        // This used to be orElseThrow(NotFoundException). An attempt that was forcibly ended and then cleared of the violation
        // never had any evaluation, so the Result page could get neither the audio nor the transcript
        // even though assessment_item_response_turns was intact -- while the grading screen shows everything, because it LEFT JOINs
        // to the evaluation instead of demanding one. Two pages read the same data with two opposite
        // assumptions; this pulls the result page onto the assumption of the grading screen.
        //
        // The score and criteria are left empty because they really do not exist yet. The speech recording is not a score --
        // it is evidence, and there is no reason to make anyone wait until grading is done to see it.
        if (evaluation == null) {
            return ungradedDetails(access.response());
        }

        // Bản hiệu lực là bản chấm tay ⇒ mọi bằng chứng AI nằm ở bản AI đã bị SUPERSEDED,
        // phải đi tìm riêng. Bản hiệu lực đã là AI thì chính nó — không tốn thêm query.
        AssessmentItemEvaluation aiEvaluation = evaluation.getEngineType() == AssessmentEvaluationEngineType.HUMAN
            ? assessmentItemEvaluationRepository.findLatestAiByResponseId(input.answerId()).orElse(null)
            : evaluation;

        // criteria vẫn theo bản hiệu lực: sau khi chấm lại thì đây là điểm và rationale của
        // GIÁO VIÊN, đúng như thiết kế. Chỉ ngữ cảnh mới lấy từ bản AI.
        List<AssessmentItemCriterionScoreResponse> criteria = assessmentItemCriterionScoreRepository.findByEvaluationId(evaluation.getId()).stream()
            .map(item -> {
                RubricCriterion criterion = rubricCriterionRepository.findById(item.getRubricCriterionId()).orElse(null);
                return new AssessmentItemCriterionScoreResponse(
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
        UUID turnSourceId = (aiEvaluation == null ? evaluation : aiEvaluation).getId();
        List<AssessmentItemEvaluationTurnResponse> turns = assessmentItemEvaluationTurnRepository.findByEvaluationId(turnSourceId).stream()
            .map(item -> new AssessmentItemEvaluationTurnResponse(
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
        // No AI version -> fall back to the candidate's ORIGINAL spoken turns.
        //
        // An attempt forcibly ended midway was never graded by the AI, and an attempt graded by a teacher by hand
        // only produces a HUMAN version -- and HUMAN has no turns. Both land here, and the
        // Result page showed a blank question: no audio, no transcript, no prompt, even though
        // assessment_item_response_turns was intact (measured 2026-08-17, session 01a00e64).
        //
        // The three fields specific to the AI version (asrConfidence, pronunciationOverall, wordFeedback) are left
        // null: the original table does not have them, and inventing a value is worse than leaving it empty. Same handling
        // as JpaAssessmentGradingQueryRepository#turnsForItem on the grading screen.
        if (turns.isEmpty()) {
            turns = assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(evaluation.getResponseId()).stream()
                .map(item -> new AssessmentItemEvaluationTurnResponse(
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

        return new AssessmentItemEvaluationDetailsResponse(
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
    private AssessmentItemEvaluationDetailsResponse ungradedDetails(
            com.sep.vox.domain.model.assessment.AssessmentItemResponse response) {
        List<AssessmentItemEvaluationTurnResponse> turns = assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(response.getId()).stream()
            .map(item -> new AssessmentItemEvaluationTurnResponse(
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
        return new AssessmentItemEvaluationDetailsResponse(
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

    private AssessmentItemAiEvaluationContextResponse toAiContext(AssessmentItemEvaluation aiEvaluation) {
        if (aiEvaluation == null) {
            return null;
        }
        return new AssessmentItemAiEvaluationContextResponse(
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
