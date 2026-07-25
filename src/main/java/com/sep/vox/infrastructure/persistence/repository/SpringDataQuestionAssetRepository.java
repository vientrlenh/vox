package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionAssetJpaEntity;

public interface SpringDataQuestionAssetRepository extends JpaRepository<QuestionAssetJpaEntity, UUID> {
    List<QuestionAssetJpaEntity> findByQuestionIdOrderByOrderAsc(UUID questionId);
    List<QuestionAssetJpaEntity> findByQuestionIdInOrderByOrderAsc(Collection<UUID> questionIds);
    boolean existsByQuestionId(UUID questionId);
    void deleteByQuestionId(UUID questionId);
}
