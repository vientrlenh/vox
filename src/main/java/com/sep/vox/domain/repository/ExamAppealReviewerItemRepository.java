package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamAppealReviewerItem;

public interface ExamAppealReviewerItemRepository {
    List<ExamAppealReviewerItem> saveAll(List<ExamAppealReviewerItem> items);
    List<ExamAppealReviewerItem> findByAppealReviewerId(UUID appealReviewerId);
    /** Dọn tay khi xoá phiên thi — không có FK nào chặn. */
    void deleteByAppealIdIn(Collection<UUID> appealIds);
}
