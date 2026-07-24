package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamAppealReviewerJpaEntity;

public interface SpringDataExamAppealReviewerRepository extends JpaRepository<ExamAppealReviewerJpaEntity, UUID> {
    Optional<ExamAppealReviewerJpaEntity> findByAppealIdAndReviewerId(UUID appealId, UUID reviewerId);
    List<ExamAppealReviewerJpaEntity> findByAppealIdOrderByAssignedAtAsc(UUID appealId);
    long countByReviewerIdAndStatus(UUID reviewerId, String status);
    void deleteByAppealIdIn(Collection<UUID> appealIds);
}
