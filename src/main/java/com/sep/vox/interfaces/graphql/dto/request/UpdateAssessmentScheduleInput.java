package com.sep.vox.interfaces.graphql.dto.request;

import java.time.Instant;
import java.util.UUID;

public record UpdateAssessmentScheduleInput(
    UUID schoolRoomId,
    Instant startDate,
    Instant endDate
) {
}
