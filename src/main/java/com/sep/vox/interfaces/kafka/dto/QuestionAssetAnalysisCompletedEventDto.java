package com.sep.vox.interfaces.kafka.dto;

public record QuestionAssetAnalysisCompletedEventDto(
    String eventType,
    String assetId,
    Payload payload
) {
    public record Payload(String transcript, String description) {
    }
}
