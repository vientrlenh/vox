package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.infrastructure.persistence.entity.QuestionCollaboratorJpaEntity;

public final class QuestionCollaboratorMapper {

    private QuestionCollaboratorMapper() {
    }

    public static QuestionCollaborator toDomain(QuestionCollaboratorJpaEntity entity) {
        return new QuestionCollaborator(
            entity.getId(),
            entity.getUserId(),
            entity.getQuestionId(),
            QuestionCollaboratorPermission.valueOf(entity.getPermission()),
            entity.getAssignedAt()
        );
    }

    public static QuestionCollaboratorJpaEntity toJpa(QuestionCollaborator collaborator) {
        return new QuestionCollaboratorJpaEntity(
            collaborator.getId(),
            collaborator.getUserId(),
            collaborator.getQuestionId(),
            collaborator.getPermission().name(),
            collaborator.getAssignedAt()
        );
    }
}
