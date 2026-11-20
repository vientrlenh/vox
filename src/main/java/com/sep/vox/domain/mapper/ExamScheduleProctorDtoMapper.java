package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.ExamScheduleProctorDto;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;

public final class ExamScheduleProctorDtoMapper {

    private ExamScheduleProctorDtoMapper() {
    }

    public static ExamScheduleProctorDto toDto(ExamScheduleProctor domain) {
        return new ExamScheduleProctorDto(
            domain.getId(),
            domain.getScheduleId(),
            domain.getTeacherId()
        );
    }

    public static List<ExamScheduleProctorDto> toDtoList(List<ExamScheduleProctor> domains) {
        return domains.stream()
            .map(ExamScheduleProctorDtoMapper::toDto)
            .toList();
    }
}
