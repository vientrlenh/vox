package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.model.exam.ExamCandidate;

public final class ExamCandidateDtoMapper {

    private ExamCandidateDtoMapper() {
    }

    public static ExamCandidateDto toDto(ExamCandidate domain) {
        return new ExamCandidateDto(
            domain.getId(),
            domain.getExamId(),
            domain.getStudentId(),
            domain.getAssignedPaperId(),
            domain.getScheduleId(),
            domain.getStatus() == null ? null : domain.getStatus().name(),
            valueOf(domain.getAssignedAt()),
            valueOf(domain.getUpdatedAt()),
            valueOf(domain.getBlockedAt())
        );
    }

    public static List<ExamCandidateDto> toDtoList(List<ExamCandidate> domains) {
        return domains.stream()
            .map(ExamCandidateDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
