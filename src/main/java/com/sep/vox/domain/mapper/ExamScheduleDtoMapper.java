package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.model.exam.ExamSchedule;

public final class ExamScheduleDtoMapper {

    private ExamScheduleDtoMapper() {
    }

    public static ExamScheduleDto toDto(ExamSchedule domain) {
        return new ExamScheduleDto(
            domain.getId(),
            domain.getExamId(),
            domain.getSchoolRoomId(),
            valueOf(domain.getStartDate()),
            valueOf(domain.getEndDate()),
            domain.getStatus() == null ? null : domain.getStatus().name(),
            domain.getMovedToScheduleId()
        );
    }

    public static List<ExamScheduleDto> toDtoList(List<ExamSchedule> domains) {
        return domains.stream()
            .map(ExamScheduleDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
