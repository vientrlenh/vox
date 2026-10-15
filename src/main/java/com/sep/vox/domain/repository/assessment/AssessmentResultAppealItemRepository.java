package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;

public interface AssessmentResultAppealItemRepository {
    List<AssessmentResultAppealItem> saveAll(List<AssessmentResultAppealItem> items);
    List<AssessmentResultAppealItem> findByAppealId(UUID appealId);
    /** Dọn tay khi xoá phiên thi — không có FK nào chặn. */
    void deleteByAppealIdIn(Collection<UUID> appealIds);
}
