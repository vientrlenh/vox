package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;

public record AssessmentScheduleProctorDto(
    UUID id,
    UUID scheduleId,
    UUID teacherId
) {

    public static AssessmentScheduleProctorDto toDto(AssessmentScheduleProctor assessmentScheduleProctor) {
        return new AssessmentScheduleProctorDto(
            assessmentScheduleProctor.getId(),
            assessmentScheduleProctor.getScheduleId(),
            assessmentScheduleProctor.getTeacherId()
        );
    }

    public static List<AssessmentScheduleProctorDto> toDtoList(List<AssessmentScheduleProctor> domains) {
        return domains.stream()
            .map(AssessmentScheduleProctorDto::toDto)
            .toList();
    }
}
