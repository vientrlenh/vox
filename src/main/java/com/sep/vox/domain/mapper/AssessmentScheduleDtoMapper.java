package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;

public final class AssessmentScheduleDtoMapper {

    private AssessmentScheduleDtoMapper() {
    }

    public static AssessmentScheduleDto toDto(AssessmentSchedule domain) {
        return new AssessmentScheduleDto(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getSchoolRoomId(),
            domain.getStartDate(),
            domain.getEndDate(),
            domain.getStatus(),
            domain.getMovedToScheduleId()
        );
    }

    public static List<AssessmentScheduleDto> toDtoList(List<AssessmentSchedule> domains) {
        return domains.stream()
            .map(AssessmentScheduleDtoMapper::toDto)
            .toList();
    }
}
