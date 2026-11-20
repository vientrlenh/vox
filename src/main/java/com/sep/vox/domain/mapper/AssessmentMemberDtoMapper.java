package com.sep.vox.domain.mapper;

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
            domain.getRole(),
            domain.getGrantedAt(),
            domain.getGrantedBy()
        );
    }

    public static List<AssessmentMemberDto> toDtoList(List<AssessmentMember> domains) {
        return domains.stream()
            .map(AssessmentMemberDtoMapper::toDto)
            .toList();
    }
}
