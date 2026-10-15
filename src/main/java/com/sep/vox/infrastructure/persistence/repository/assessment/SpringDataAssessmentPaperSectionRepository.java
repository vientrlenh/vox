package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperSectionJpaEntity;

public interface SpringDataAssessmentPaperSectionRepository extends JpaRepository<AssessmentPaperSectionJpaEntity, UUID> {
    List<AssessmentPaperSectionJpaEntity> findByPaperIdOrderByOrderAsc(UUID paperId);
    List<AssessmentPaperSectionJpaEntity> findByPaperIdInOrderByOrderAsc(Collection<UUID> paperIds);
    void deleteByPaperIdIn(Collection<UUID> paperIds);
}
