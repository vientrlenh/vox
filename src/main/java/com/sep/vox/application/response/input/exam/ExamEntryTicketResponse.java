package com.sep.vox.application.response.input.exam;

import java.util.UUID;

public record ExamEntryTicketResponse(
    UUID attemptId,
    String ticketId,
    String expiresAt,
    String scheduleEndAt
) {
}
