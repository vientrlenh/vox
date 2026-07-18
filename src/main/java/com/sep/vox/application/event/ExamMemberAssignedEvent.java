package com.sep.vox.application.event;

import java.util.UUID;

public record ExamMemberAssignedEvent(
    UUID examId,
    UUID teacherUserId,
    String role
) {
}
