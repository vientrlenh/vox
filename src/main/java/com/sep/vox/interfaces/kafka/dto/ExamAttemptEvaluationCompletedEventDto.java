package com.sep.vox.interfaces.kafka.dto;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

public record ExamAttemptEvaluationCompletedEventDto(
    String eventType,
    Integer schemaVersion,
    String examAttemptId,
    String answerId,
    String questionId,
    PayloadDto payload
) {
    public record PayloadDto(
        List<TurnDetailDto> turns,
        Map<String, CriterionScoreDto> criteria,
        EvaluationSignalsDto signals,
        ValidityResultDto validity,
        String feedbackSummary,
        List<JsonNode> suggestions,
        String modelVersion,
        String promptVersion,
        String evaluatedAt
    ) {
    }

    public record CriterionScoreDto(
        Double score,
        String level,
        String status,
        String source,
        Map<String, Object> subscores,
        String note,
        String suggestion
    ) {
    }

    public record EvaluationSignalsDto(
        Integer durationSeconds,
        Integer wordCount,
        Integer sentenceCount,
        Double lengthRatio,
        Integer expectedMinWords,
        Double asrConfidenceAvg,
        Double topicRelevanceScore,
        Double offTopicRatio,
        Double codeSwitchingRatio,
        Double speechRate,
        Double aiConfidence,
        Double audioQuality,
        Double silenceRatio
    ) {
    }

    public record ValidityResultDto(
        Boolean validForScoring,
        String action,
        String overallSeverity,
        List<JsonNode> ruleResults,
        List<JsonNode> flags,
        Map<String, Object> scoreCaps,
        List<JsonNode> penalties,
        List<String> notes,
        String transcriptSource,
        Integer transcriptWordCount
    ) {
    }

    public record TurnDetailDto(
        String turnId,
        Integer turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        Integer wordCount,
        Integer durationSeconds,
        Double asrConfidence,
        PronunciationOverallDto pronunciationOverall,
        List<JsonNode> wordFeedback
    ) {
    }

    public record PronunciationOverallDto(
        Double accuracyScore,
        Double fluencyScore,
        Double prosodyScore,
        Double pronScore,
        Double completenessScore
    ) {
    }
}
