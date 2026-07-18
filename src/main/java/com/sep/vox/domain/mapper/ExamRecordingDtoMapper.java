package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.ExamRecordingDto;
import com.sep.vox.domain.repository.ExamRecordingEntry;

public final class ExamRecordingDtoMapper {

    private ExamRecordingDtoMapper() {
    }

    public static ExamRecordingDto toDto(ExamRecordingEntry entry) {
        var domain = entry.response();
        return new ExamRecordingDto(
            domain.getId(),
            entry.examId(),
            domain.getAudioUrl(),
            domain.getDurationSeconds(),
            domain.getTranscript(),
            valueOf(domain.getSubmittedAt())
        );
    }

    public static List<ExamRecordingDto> toDtoList(List<ExamRecordingEntry> entries) {
        return entries.stream()
            .map(ExamRecordingDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
