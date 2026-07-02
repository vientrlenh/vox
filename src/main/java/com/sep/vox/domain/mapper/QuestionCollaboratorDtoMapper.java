package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.model.question.QuestionCollaborator;

public final class QuestionCollaboratorDtoMapper {

    private QuestionCollaboratorDtoMapper() {
    }

    public static QuestionCollaboratorDto toDto(QuestionCollaborator collaborator) {
        return new QuestionCollaboratorDto(
            collaborator.getId(),
            collaborator.getUserId(),
            collaborator.getQuestionId(),
            collaborator.getPermission().name(),
            valueOf(collaborator.getAssignedAt())
        );
    }

    public static List<QuestionCollaboratorDto> toDtoList(List<QuestionCollaborator> collaborators) {
        return collaborators.stream()
            .map(QuestionCollaboratorDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
