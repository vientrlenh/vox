package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionCollaborator;

public interface QuestionCollaboratorRepository {
    QuestionCollaborator save(QuestionCollaborator collaborator);
    Optional<QuestionCollaborator> findById(UUID id);
    Optional<QuestionCollaborator> findByQuestionIdAndUserId(UUID questionId, UUID userId);
    List<QuestionCollaborator> findByQuestionId(UUID questionId);
    void deleteById(UUID id);
    void deleteByQuestionId(UUID questionId);
}
