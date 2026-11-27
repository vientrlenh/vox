package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record PracticePaperDto(
    UUID id,
    UUID topicId,
    String origin,
    int plannedSeconds,
    int reservedQuotaSeconds,
    /**
     * Speaking budget for the WHOLE session (seconds): the narrower of the plan quota and the band ceiling.
     *
     * Different from {@code plannedSeconds} just above: that one is the planned total of the questions in this
     * paper, this one is the ceiling for the whole session. The client needs it as soon as the paper opens to draw the
     * "spoken / budget" bar -- the first question has not submitted any turn yet, so no other source exists.
     */
    int sessionBudgetSeconds,
    List<PracticePaperQuestionDto> questions
) {
}
