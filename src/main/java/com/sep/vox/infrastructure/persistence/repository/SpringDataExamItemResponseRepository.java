package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseJpaEntity;

public interface SpringDataExamItemResponseRepository extends JpaRepository<ExamItemResponseJpaEntity, UUID> {
    List<ExamItemResponseJpaEntity> findBySessionId(UUID sessionId);
}
