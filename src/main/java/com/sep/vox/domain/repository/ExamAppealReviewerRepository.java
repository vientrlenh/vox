package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamAppealReviewer;

public interface ExamAppealReviewerRepository {
    Optional<ExamAppealReviewer> findById(UUID id);
    Optional<ExamAppealReviewer> findByAppealIdAndReviewerId(UUID appealId, UUID reviewerId);
    List<ExamAppealReviewer> findByAppealId(UUID appealId);
    List<ExamAppealReviewer> saveAll(List<ExamAppealReviewer> reviewers);
    ExamAppealReviewer save(ExamAppealReviewer reviewer);
    void deleteById(UUID id);
    void deleteByAppealIdIn(Collection<UUID> appealIds);

    /** Số việc chấm lại một giám khảo đang giữ (status = ASSIGNED) — cột `load` của picker phân công. */
    long countAssignedByReviewerId(UUID reviewerId);
}
