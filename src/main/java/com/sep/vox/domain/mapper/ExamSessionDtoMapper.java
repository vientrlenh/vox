package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.ExamSessionDto;
import com.sep.vox.domain.model.exam.ExamSession;

public final class ExamSessionDtoMapper {

    private ExamSessionDtoMapper() {
    }

    public static ExamSessionDto toDto(ExamSession domain) {
        return new ExamSessionDto(
            domain.getId(),
            domain.getExamId(),
            domain.getCandidateId(),
            domain.getPaperId(),
            valueOf(domain.getStartedAt()),
            valueOf(domain.getSubmittedAt()),
            domain.getStatus() == null ? null : domain.getStatus().name()
        );
    }

    public static List<ExamSessionDto> toDtoList(List<ExamSession> domains) {
        return domains.stream()
            .map(ExamSessionDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
