package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record TeacherPracticeSessionDetailDto(
    UUID sessionId,
    String topicName,
    String startedAt,
    int durationSeconds,
    int itemCount,
    Double overallScore,
    List<PracticeCriterionScoreDto> criterionScores,
    boolean completed,
    List<TeacherPracticeTurnViewDto> turns
) {
}
