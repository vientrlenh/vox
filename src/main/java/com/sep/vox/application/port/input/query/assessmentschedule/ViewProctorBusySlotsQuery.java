package com.sep.vox.application.port.input.query.assessmentschedule;

import java.util.List;
import java.util.UUID;

/** Within the group {@code teacherIds}, who has a schedule conflict with the time slot of schedule {@code scheduleId}. */
public record ViewProctorBusySlotsQuery(
    UUID scheduleId,
    List<UUID> teacherIds
) {
}
