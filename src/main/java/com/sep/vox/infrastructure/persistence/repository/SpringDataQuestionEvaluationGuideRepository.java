package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionEvaluationGuideJpaEntity;

public interface SpringDataQuestionEvaluationGuideRepository extends JpaRepository<QuestionEvaluationGuideJpaEntity, UUID> {
    Optional<QuestionEvaluationGuideJpaEntity> findByQuestionId(UUID questionId);
    List<QuestionEvaluationGuideJpaEntity> findByQuestionIdIn(Collection<UUID> questionIds);
    void deleteByQuestionId(UUID questionId);
}
