package com.sep.vox.application.response.input.assessment;

import com.sep.vox.domain.dto.ExamDto;

public record CreateClassTestResponse(
    ExamDto exam,
    int candidateCount
) {
}
