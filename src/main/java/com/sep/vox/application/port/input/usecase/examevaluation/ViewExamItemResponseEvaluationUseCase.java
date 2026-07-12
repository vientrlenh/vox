package com.sep.vox.application.port.input.usecase.examevaluation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemCriterionScoreResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationTurnResponse;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ViewExamItemResponseEvaluationUseCase
        implements IUseCase<ViewExamItemResponseEvaluationQuery, ExamItemEvaluationDetailsResponse> {

    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ExamResultAccessService examResultAccessService;
    private final JsonMapper jsonMapper;

    public ViewExamItemResponseEvaluationUseCase(
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ExamResultAccessService examResultAccessService,
            JsonMapper jsonMapper) {
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examItemEvaluationTurnRepository = examItemEvaluationTurnRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.examResultAccessService = examResultAccessService;
        this.jsonMapper = jsonMapper;
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
                readJson(item.getPronunciationOverallJson()),
                readJson(item.getWordFeedbackJson())
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
            readJson(writeJson(evaluation.getSignals())),
            readJson(evaluation.getValidityJson()),
            readJson(evaluation.getSuggestionsJson()),
            criteria,
            turns
        );
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readTree(value);
        } catch (Exception ex) {
            throw new IllegalStateException("khong the doc du lieu json cua evaluation", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("khong the serialize du lieu json cua evaluation", ex);
        }
    }
}
