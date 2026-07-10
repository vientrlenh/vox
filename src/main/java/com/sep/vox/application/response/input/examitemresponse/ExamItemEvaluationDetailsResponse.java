package com.sep.vox.application.response.input.examitemresponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record ExamItemEvaluationDetailsResponse(
    UUID id,
    UUID responseId,
    UUID paperItemId,
    String engineType,
    String gradedByModel,
    String promptVersion,
    BigDecimal rawItemScore,
    BigDecimal itemScore,
    BigDecimal overallConfidence,
    boolean markedInvalid,
    boolean requiresRetake,
    String status,
    String evaluatedAt,
    String feedbackSummary,
    JsonNode signals,
    JsonNode suggestions,
    List<ExamItemCriterionScoreResponse> criteria,
    List<ExamItemEvaluationTurnResponse> turns
) {
}
