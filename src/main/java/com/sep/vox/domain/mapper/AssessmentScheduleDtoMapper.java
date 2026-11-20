package com.sep.vox.domain.mapper;

import java.time.Instant;
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
            valueOf(domain.getStartDate()),
            valueOf(domain.getEndDate()),
            domain.getStatus() == null ? null : domain.getStatus().name(),
            domain.getMovedToScheduleId()
        );
    }

    public static List<AssessmentScheduleDto> toDtoList(List<AssessmentSchedule> domains) {
        return domains.stream()
            .map(AssessmentScheduleDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
