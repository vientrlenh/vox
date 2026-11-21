package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionCollaboratorJpaEntity;

public final class QuestionCollaboratorMapper {

    private QuestionCollaboratorMapper() {
    }

    public static QuestionCollaborator toDomain(QuestionCollaboratorJpaEntity entity) {
        return QuestionCollaborator.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .questionId(entity.getQuestionId())
                .permission(QuestionCollaboratorPermission.valueOf(entity.getPermission()))
                .assignedAt(entity.getAssignedAt())
                .build();
    }

    public static QuestionCollaboratorJpaEntity toJpa(QuestionCollaborator collaborator) {
        return QuestionCollaboratorJpaEntity.builder()
                .id(collaborator.getId())
                .userId(collaborator.getUserId())
                .questionId(collaborator.getQuestionId())
                .permission(collaborator.getPermission().name())
                .assignedAt(collaborator.getAssignedAt())
                .build();
    }
}
