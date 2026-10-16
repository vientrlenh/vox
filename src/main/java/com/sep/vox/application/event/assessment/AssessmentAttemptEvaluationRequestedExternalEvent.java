package com.sep.vox.application.event.assessment;

import java.util.List;

import com.sep.vox.application.event.ExternalEventTopic;
import com.sep.vox.application.event.HasPartitionKey;

@ExternalEventTopic("assessment-attempt-evaluation-requested")
public record AssessmentAttemptEvaluationRequestedExternalEvent(
    String eventType,
    Integer schemaVersion,
    String assessmentAttemptId,
    String answerId,
    String questionId,
    Payload payload
) implements HasPartitionKey {
    public AssessmentAttemptEvaluationRequestedExternalEvent(
            String assessmentAttemptId,
            String answerId,
            String questionId,
            Payload payload) {
        this("AssessmentAttemptEvaluationRequested", 1, assessmentAttemptId, answerId, questionId, payload);
    }

    @Override
    public String partitionKey() {
        return answerId();
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
        List<String> negativeSignals,
        Integer order
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
