package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.model.assessment.AssessmentMember;

public final class AssessmentMemberDtoMapper {

    private AssessmentMemberDtoMapper() {
    }

    public static AssessmentMemberDto toDto(AssessmentMember domain) {
        return new AssessmentMemberDto(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getUserId(),
            domain.getRole().name(),
            valueOf(domain.getGrantedAt()),
            domain.getGrantedBy()
        );
    }

    public static List<AssessmentMemberDto> toDtoList(List<AssessmentMember> domains) {
        return domains.stream()
            .map(AssessmentMemberDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
