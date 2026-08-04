package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamPaperItemJpaEntity;

public interface SpringDataExamPaperItemRepository extends JpaRepository<ExamPaperItemJpaEntity, UUID> {
    List<ExamPaperItemJpaEntity> findBySectionIdOrderByOrderAsc(UUID sectionId);
    List<ExamPaperItemJpaEntity> findBySectionIdInOrderByOrderAsc(Collection<UUID> sectionIds);

    List<ExamPaperItemJpaEntity> findByPaperId(UUID paperId);
    List<ExamPaperItemJpaEntity> findByPaperIdIn(Collection<UUID> paperIds);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM ExamPaperItemJpaEntity i
        WHERE i.paperId = :paperId AND i.questionId IS NULL
    """)
    boolean existsUnassignedItemByPaperId(@Param("paperId") UUID paperId);
}
