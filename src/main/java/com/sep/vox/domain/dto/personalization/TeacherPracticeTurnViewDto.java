package com.sep.vox.domain.dto.personalization;

import java.util.List;

public record TeacherPracticeTurnViewDto(
    int turnOrder,
    String transcript,
    String audioUrl,
    String wordFeedbackJson,
    Double turnScore,
    List<TurnCorrectionDto> corrections
) {
}
