package com.sep.vox.domain.dto.assessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;

public record AssessmentScheduleDto(
    UUID id,
    UUID assessmentId,
    UUID schoolRoomId,
    Instant startDate,
    Instant endDate,
    AssessmentScheduleStatus status,
    UUID movedToScheduleId
) {

    public static AssessmentScheduleDto toDto(AssessmentSchedule assessmentSchedule) {
        return new AssessmentScheduleDto(
            assessmentSchedule.getId(),
            assessmentSchedule.getAssessmentId(),
            assessmentSchedule.getSchoolRoomId(),
            assessmentSchedule.getStartDate(),
            assessmentSchedule.getEndDate(),
            assessmentSchedule.getStatus(),
            assessmentSchedule.getMovedToScheduleId()
        );
    }

    public static List<AssessmentScheduleDto> toDtoList(List<AssessmentSchedule> domains) {
        return domains.stream()
            .map(AssessmentScheduleDto::toDto)
            .toList();
    }
}
