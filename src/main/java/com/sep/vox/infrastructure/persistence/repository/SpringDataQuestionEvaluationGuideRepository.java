package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionEvaluationGuideJpaEntity;

public interface SpringDataQuestionEvaluationGuideRepository extends JpaRepository<QuestionEvaluationGuideJpaEntity, UUID> {
}
