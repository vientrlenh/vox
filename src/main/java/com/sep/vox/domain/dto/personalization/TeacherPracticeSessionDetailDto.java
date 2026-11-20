package com.sep.vox.domain.dto.personalization;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeacherPracticeSessionDetailDto(
    UUID sessionId,
    String topicName,
    Instant startedAt,
    int durationSeconds,
    int itemCount,
    Double overallScore,
    List<PracticeCriterionScoreDto> criterionScores,
    boolean completed,
    int pendingEvaluationCount,
    int gradingGaveUpCount,
    Double difficultyRank,
    List<TeacherPracticeTurnViewDto> turns,
    double scoreScaleMin,
    double scoreScaleMax
) {
}
