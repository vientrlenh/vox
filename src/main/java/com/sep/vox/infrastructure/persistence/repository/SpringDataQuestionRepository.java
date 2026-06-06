package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;

public interface SpringDataQuestionRepository extends JpaRepository<QuestionJpaEntity, UUID> {
    List<QuestionJpaEntity> findByQuestionTopicId(UUID topicId);
    Page<QuestionJpaEntity> findByQuestionTopicId(UUID topicId, Pageable pageable);
}
