package com.sep.vox.application.response.input.exam;

import java.util.List;
import java.util.UUID;

public record StudentExamPaperResponse(
    UUID examId,
    UUID examPaperId,
    String title,
    String subject,
    String description,
    int durationSeconds,
    int durationMinutes,
    String examDate,
    String status,
    String scheduleEndAt,
    List<StudentExamPaperQuestionResponse> paperQuestions
) {
}
