package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperItemJpaEntity;

public interface SpringDataAssessmentPaperItemRepository extends JpaRepository<AssessmentPaperItemJpaEntity, UUID> {
    List<AssessmentPaperItemJpaEntity> findBySectionIdOrderByOrderAsc(UUID sectionId);
    List<AssessmentPaperItemJpaEntity> findBySectionIdInOrderByOrderAsc(Collection<UUID> sectionIds);

    List<AssessmentPaperItemJpaEntity> findByPaperId(UUID paperId);
    List<AssessmentPaperItemJpaEntity> findByPaperIdIn(Collection<UUID> paperIds);
    void deleteByPaperIdIn(Collection<UUID> paperIds);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM AssessmentPaperItemJpaEntity i
        WHERE i.paperId = :paperId AND i.questionId IS NULL
    """)
    boolean existsUnassignedItemByPaperId(@Param("paperId") UUID paperId);
}
