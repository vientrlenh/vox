package com.sep.vox.application.port.input.usecase.examevaluation;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.examitemresponse.ExamItemCriterionScoreResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationTurnResponse;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
public class ViewExamItemResponseEvaluationUseCase
        implements IUseCase<ViewExamItemResponseEvaluationQuery, ExamItemEvaluationDetailsResponse> {

    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ExamResultAccessService examResultAccessService;
    private final JsonSerializationPort jsonSerializationPort;

    public ViewExamItemResponseEvaluationUseCase(
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ExamResultAccessService examResultAccessService,
            JsonSerializationPort jsonSerializationPort) {
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examItemEvaluationTurnRepository = examItemEvaluationTurnRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.examResultAccessService = examResultAccessService;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ExamItemEvaluationDetailsResponse execute(ViewExamItemResponseEvaluationQuery input) {
        examResultAccessService.getAuthorizedResponse(input.answerId());
        var evaluation = examItemEvaluationRepository.findLatestByResponseId(input.answerId())
            .orElseThrow(() -> new NotFoundException("khong the tim thay evaluation cho cau tra loi"));
        var criteria = examItemCriterionScoreRepository.findByEvaluationId(evaluation.getId()).stream()
            .map(item -> {
                var criterion = rubricCriterionRepository.findById(item.getRubricCriterionId()).orElse(null);
                return new ExamItemCriterionScoreResponse(
                    item.getId(),
                    item.getRubricCriterionId(),
                    criterion == null ? null : criterion.getCode(),
                    criterion == null ? null : criterion.getName(),
                    item.getRawScore(),
                    item.getFinalScore(),
                    item.getRationale()
                );
            })
            .toList();
        var turns = examItemEvaluationTurnRepository.findByEvaluationId(evaluation.getId()).stream()
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
            turns
        );
    }
}
