package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PracticeSession(
    UUID id,
    UUID studentId,
    UUID practicePaperId,
    UUID rubricVersionId,
    UUID targetFrameworkBandId,
    UUID chosenPracticeTopicId,
    String targetSubAttributesJson,
    String origin,
    String offeredTopicIdsJson,
    BigDecimal overallScore,
    OffsetDateTime startedAt,
    OffsetDateTime endedAt,
    OffsetDateTime lastHeartbeatAt,
    int gradedSeconds,
    String status,
    String abandonDiagnosis,
    int helpRequestCount,
    int longPauseCount
) {
    public PracticeSession withLastHeartbeatAt(OffsetDateTime newLastHeartbeatAt) {
        return new PracticeSession(
            id, studentId, practicePaperId, rubricVersionId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            overallScore, startedAt, endedAt, newLastHeartbeatAt, gradedSeconds, status,
            abandonDiagnosis, helpRequestCount, longPauseCount
        );
    }

    public PracticeSession withGradedSecondsAndHeartbeat(int newGradedSeconds, OffsetDateTime newLastHeartbeatAt) {
        return new PracticeSession(
            id, studentId, practicePaperId, rubricVersionId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            overallScore, startedAt, endedAt, newLastHeartbeatAt, newGradedSeconds, status,
            abandonDiagnosis, helpRequestCount, longPauseCount
        );
    }

    public PracticeSession ended(
            String newStatus,
            String newAbandonDiagnosis,
            int newHelpRequestCount,
            int newLongPauseCount,
            OffsetDateTime newEndedAt,
            BigDecimal newOverallScore) {
        return new PracticeSession(
            id, studentId, practicePaperId, rubricVersionId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            newOverallScore, startedAt, newEndedAt, lastHeartbeatAt, gradedSeconds, newStatus,
            newAbandonDiagnosis, newHelpRequestCount, newLongPauseCount
        );
    }

    public PracticeSession closedAsStale(String newStatus, String newAbandonDiagnosis, OffsetDateTime newEndedAt) {
        return new PracticeSession(
            id, studentId, practicePaperId, rubricVersionId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            overallScore, startedAt, newEndedAt, lastHeartbeatAt, gradedSeconds, newStatus,
            newAbandonDiagnosis, helpRequestCount, longPauseCount
        );
    }
}
