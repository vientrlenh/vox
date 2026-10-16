package com.sep.vox.application.response.input.assessment;

import com.sep.vox.domain.dto.AssessmentDto;

public record CreateClassTestResponse(
    AssessmentDto assessment,
    int candidateCount
) {
}
