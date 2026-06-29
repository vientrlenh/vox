package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;

import com.sep.vox.domain.dto.ExamSecurePoolDto;
import com.sep.vox.domain.model.exam.ExamSecurePool;

public final class ExamSecurePoolDtoMapper {

    private ExamSecurePoolDtoMapper() {
    }

    public static ExamSecurePoolDto toDto(ExamSecurePool domain) {
        return new ExamSecurePoolDto(
            domain.getId(),
            domain.getExamId(),
            domain.getStatus().name(),
            domain.getReleaseMode().name(),
            valueOf(domain.getEmbargoUntil()),
            valueOf(domain.getReleasedAt()),
            domain.getReleasedBy()
        );
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
