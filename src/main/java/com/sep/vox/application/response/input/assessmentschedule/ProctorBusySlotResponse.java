package com.sep.vox.application.response.input.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

/**
 * A teacher who is already booked: they proctor session {@code scheduleId}, which runs from {@code startDate} to
 * {@code endDate}. The UI uses it to grey out busy teachers in the picker list together with the reason, instead of
 * letting the user click and run into an error.
 */
public record ProctorBusySlotResponse(
    UUID teacherId,
    UUID scheduleId,
    Instant startDate,
    Instant endDate
) {
}
