package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.model.exam.ExamMember;

public final class ExamMemberDtoMapper {

    private ExamMemberDtoMapper() {
    }

    public static ExamMemberDto toDto(ExamMember domain) {
        return new ExamMemberDto(
            domain.getId(),
            domain.getExamId(),
            domain.getUserId(),
            domain.getRole().name(),
            valueOf(domain.getGrantedAt()),
            domain.getGrantedBy()
        );
    }

    public static List<ExamMemberDto> toDtoList(List<ExamMember> domains) {
        return domains.stream()
            .map(ExamMemberDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
