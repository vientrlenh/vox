package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamPaperSectionJpaEntity;

public interface SpringDataExamPaperSectionRepository extends JpaRepository<ExamPaperSectionJpaEntity, UUID> {
    List<ExamPaperSectionJpaEntity> findByPaperIdOrderByOrderAsc(UUID paperId);
    List<ExamPaperSectionJpaEntity> findByPaperIdInOrderByOrderAsc(Collection<UUID> paperIds);
    void deleteByPaperIdIn(Collection<UUID> paperIds);
}
