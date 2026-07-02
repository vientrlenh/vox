package com.sep.vox.application.response.input.exam;

import java.util.UUID;

import com.sep.vox.domain.dto.ExamDto;

public record CreateClassTestResponse(
    ExamDto exam,
    UUID paperId,
    int candidateCount
) {
}
