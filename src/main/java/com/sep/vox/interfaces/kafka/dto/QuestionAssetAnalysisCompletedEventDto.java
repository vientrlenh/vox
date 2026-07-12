package com.sep.vox.interfaces.kafka.dto;

public record QuestionAssetAnalysisCompletedEventDto(
    String eventType,
    Integer schemaVersion,
    String assetId,
    Payload payload
) {
    public record Payload(
        String transcript,
        String description
    ) {
    }
}
