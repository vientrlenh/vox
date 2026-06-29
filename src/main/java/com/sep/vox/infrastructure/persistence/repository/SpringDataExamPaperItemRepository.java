package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamPaperItemJpaEntity;

public interface SpringDataExamPaperItemRepository extends JpaRepository<ExamPaperItemJpaEntity, UUID> {
    List<ExamPaperItemJpaEntity> findBySectionIdOrderByOrderAsc(UUID sectionId);
}
