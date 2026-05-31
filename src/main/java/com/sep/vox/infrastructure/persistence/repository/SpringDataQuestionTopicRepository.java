package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;

public interface SpringDataQuestionTopicRepository extends JpaRepository<QuestionTopicJpaEntity, UUID> {
    List<QuestionTopicJpaEntity> findByBankId(UUID bankId);
    Page<QuestionTopicJpaEntity> findByBankId(UUID bankId, Pageable pageable);
}
