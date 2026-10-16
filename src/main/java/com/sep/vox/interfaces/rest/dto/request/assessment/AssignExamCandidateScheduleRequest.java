package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

public record AssignExamCandidateScheduleRequest(
    UUID scheduleId
) {
}
