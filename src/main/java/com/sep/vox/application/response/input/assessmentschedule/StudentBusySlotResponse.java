package com.sep.vox.application.response.input.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

/**
 * A student who is already booked when being considered for session {@code targetScheduleId}: they are already
 * assigned to session {@code busyScheduleId}, which runs from {@code startDate} to {@code endDate}.
 *
 * <p>The UI uses it to grey out clashing choices together with the reason, instead of letting the user click and run
 * into an error. The name of the clashing assessment is deliberately left out: whoever schedules this assessment may
 * not have permission to see the other one.
 */
public record StudentBusySlotResponse(
    UUID studentId,
    UUID targetScheduleId,
    UUID busyScheduleId,
    Instant startDate,
    Instant endDate
) {
}
