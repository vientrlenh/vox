package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

public record UpdateExamScheduleInput(
    UUID schoolRoomId,
    String startDate,
    String endDate
) {
}
