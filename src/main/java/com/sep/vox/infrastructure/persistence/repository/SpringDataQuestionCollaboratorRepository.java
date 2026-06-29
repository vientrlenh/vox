package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionCollaboratorJpaEntity;

public interface SpringDataQuestionCollaboratorRepository extends JpaRepository<QuestionCollaboratorJpaEntity, UUID> {
    Optional<QuestionCollaboratorJpaEntity> findByQuestionIdAndUserId(UUID questionId, UUID userId);
    List<QuestionCollaboratorJpaEntity> findByQuestionId(UUID questionId);
    void deleteByQuestionId(UUID questionId);
}
