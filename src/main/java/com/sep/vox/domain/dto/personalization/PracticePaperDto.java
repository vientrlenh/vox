package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record PracticePaperDto(
    UUID id,
    UUID topicId,
    String origin,
    int plannedSeconds,
    int reservedQuotaSeconds,
    List<PracticePaperQuestionDto> questions
) {
}
