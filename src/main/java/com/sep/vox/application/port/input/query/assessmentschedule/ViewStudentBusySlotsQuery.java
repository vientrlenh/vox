package com.sep.vox.application.port.input.query.assessmentschedule;

import java.util.List;
import java.util.UUID;

/**
 * Within the group {@code studentIds}, who has a schedule conflict with the time slot of each schedule in
 * {@code scheduleIds}.
 *
 * <p>Plural on both sides because the screen needs to ask along two opposite axes: "which schedule to put ONE student into"
 * (many schedules, one student) and "add MANY students to this schedule" (one schedule, many students). One
 * query serves both instead of making the UI call in a loop.
 */
public record ViewStudentBusySlotsQuery(
    List<UUID> scheduleIds,
    List<UUID> studentIds
) {
}
