package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamResultAppealItem;

public interface ExamResultAppealItemRepository {
    List<ExamResultAppealItem> saveAll(List<ExamResultAppealItem> items);
    List<ExamResultAppealItem> findByAppealId(UUID appealId);
    /** Dọn tay khi xoá phiên thi — không có FK nào chặn. */
    void deleteByAppealIdIn(Collection<UUID> appealIds);
}
