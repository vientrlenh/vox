package com.sep.vox.application.event;

@ExternalEventTopic("question-asset-analysis-requested")
public record QuestionAssetAnalysisRequestedExternalEvent(
    String eventType,
    Integer schemaVersion,
    String assetId,
    String questionId,
    Payload payload
) {
    public QuestionAssetAnalysisRequestedExternalEvent(
            String assetId,
            String questionId,
            Payload payload) {
        this("QuestionAssetAnalysisRequested", 1, assetId, questionId, payload);
    }

    public record Payload(
        String assetType,
        String url,
        String questionText,
        EvaluationGuide evaluationGuide,
        String existingTranscript,
        String existingDescription
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
}
