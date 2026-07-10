package com.sep.vox.interfaces.graphql.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateExamScheduleInput(
    UUID schoolRoomId,
    OffsetDateTime startDate,
    OffsetDateTime endDate
) {
}
