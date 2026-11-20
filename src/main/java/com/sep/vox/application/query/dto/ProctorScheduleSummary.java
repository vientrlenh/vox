package com.sep.vox.application.query.dto;

import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import java.time.Instant;
import java.util.UUID;

public record ProctorScheduleSummary(
    UUID scheduleId,
    UUID assessmentId,
    String assessmentName,
    UUID schoolRoomId,
    String roomName,
    Instant startDate,
    Instant endDate,
    AssessmentScheduleStatus status
) {
    // Auxiliary constructor for the JPQL "SELECT NEW ...ProctorScheduleSummary(...)" projections: the status is a plain String column
    // on the JPA entity, so this overload takes the raw string and converts it here, keeping the record itself enum-typed.
    public ProctorScheduleSummary(
            UUID scheduleId,
            UUID assessmentId,
            String assessmentName,
            UUID schoolRoomId,
            String roomName,
            Instant startDate,
            Instant endDate,
            String status) {
        this(scheduleId, assessmentId, assessmentName, schoolRoomId, roomName, startDate, endDate,
            status == null ? null : AssessmentScheduleStatus.valueOf(status));
    }
}
