package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;

public record QuestionCollaboratorDto(
    UUID id,
    UUID userId,
    UUID questionId,
    QuestionCollaboratorPermission permission,
    Instant assignedAt
) {

    public static QuestionCollaboratorDto toDto(QuestionCollaborator questionCollaborator) {
        return new QuestionCollaboratorDto(
            questionCollaborator.getId(),
            questionCollaborator.getUserId(),
            questionCollaborator.getQuestionId(),
            questionCollaborator.getPermission(),
            questionCollaborator.getAssignedAt()
        );
    }

    public static List<QuestionCollaboratorDto> toDtoList(List<QuestionCollaborator> collaborators) {
        return collaborators.stream()
            .map(QuestionCollaboratorDto::toDto)
            .toList();
    }
}
