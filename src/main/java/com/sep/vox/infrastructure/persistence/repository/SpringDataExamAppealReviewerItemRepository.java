package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamAppealReviewerItemJpaEntity;

public interface SpringDataExamAppealReviewerItemRepository
        extends JpaRepository<ExamAppealReviewerItemJpaEntity, UUID> {

    List<ExamAppealReviewerItemJpaEntity> findByAppealReviewerIdOrderByIdAsc(UUID appealReviewerId);

    /** Dòng con treo qua exam_appeal_reviewers, nên phải bắc cầu bằng subquery. */
    @Modifying
    @Query("""
        DELETE FROM ExamAppealReviewerItemJpaEntity ri
        WHERE ri.appealReviewerId IN (
            SELECT r.id FROM ExamAppealReviewerJpaEntity r WHERE r.appealId IN :appealIds)
    """)
    void deleteByAppealIdIn(@Param("appealIds") Collection<UUID> appealIds);
}
