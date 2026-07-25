package com.sep.vox.application.event;

import java.util.List;

@ExternalEventTopic("exam-attempt-evaluation-requested")
public record ExamAttemptEvaluationRequestedExternalEvent(
    String eventType,
    Integer schemaVersion,
    String examAttemptId,
    String answerId,
    String questionId,
    Payload payload
) {
    public ExamAttemptEvaluationRequestedExternalEvent(
            String examAttemptId,
            String answerId,
            String questionId,
            Payload payload) {
        this("ExamAttemptEvaluationRequested", 1, examAttemptId, answerId, questionId, payload);
    }

    public record Payload(
        String questionText,
        String questionType,
        String difficultyLevel,
        Integer durationSeconds,
        Integer minResponseSeconds,
        Integer maxResponseSeconds,
        Asset asset,
        String topicName,
        String topicDescription,
        EvaluationGuide evaluationGuide,
        String mode,
        String referenceText,
        String language,
        List<CriterionFramework> criteriaFrameworks,
        List<TurnInput> turns
    ) {
    }

    public record Asset(
        String type,
        String transcript,
        String description,
        String altText
    ) {
    }

    public record EvaluationGuide(
        String expectedContent,
        String keyPoints,
        String acceptableResponses,
        String offTopicExamples,
        String scoringHints,
        String commonMistakes
    ) {
    }

    public record CriterionFramework(
        String criterionKey,
        String frameworkCode,
        String frameworkCriterionName,
        String frameworkCriterionDescription,
        String targetBandId,
        String targetBandCode,
        String targetBandLabel,
        Boolean targetBandOnly,
        Double rubricWeight,
        Double rubricMinScore,
        Double rubricMaxScore,
        List<FrameworkBand> bands
    ) {
    }

    public record FrameworkBand(
        String code,
        String label,
        Double scoreMin,
        Double scoreMax,
        String descriptor,
        List<String> positiveSignals,
        List<String> negativeSignals
    ) {
    }

    public record TurnInput(
        Integer turnOrder,
        String turnType,
        String promptText,
        String audioRef,
        String transcript,
        Integer durationSeconds
    ) {
    }
}
