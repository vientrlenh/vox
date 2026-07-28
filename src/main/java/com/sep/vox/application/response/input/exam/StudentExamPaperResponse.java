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
    /**
     * Checkpoint đồng hồ đếm ngược, null nếu phiên thi chưa checkpoint lần nào - client hiểu là
     * "đếm từ durationSeconds". Không suy ra được từ startedAt vì đồng hồ dừng lúc avatar nói.
     */
    Integer remainingSeconds,
    String startedAt,
    List<StudentExamPaperQuestionResponse> paperQuestions
) {
}
