package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.AssessmentScheduleProctorDto;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;

public final class AssessmentScheduleProctorDtoMapper {

    private AssessmentScheduleProctorDtoMapper() {
    }

    public static AssessmentScheduleProctorDto toDto(AssessmentScheduleProctor domain) {
        return new AssessmentScheduleProctorDto(
            domain.getId(),
            domain.getScheduleId(),
            domain.getTeacherId()
        );
    }

    public static List<AssessmentScheduleProctorDto> toDtoList(List<AssessmentScheduleProctor> domains) {
        return domains.stream()
            .map(AssessmentScheduleProctorDtoMapper::toDto)
            .toList();
    }
}
