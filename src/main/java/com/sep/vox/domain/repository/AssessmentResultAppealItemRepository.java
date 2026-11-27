package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;

public interface AssessmentResultAppealItemRepository {
    List<AssessmentResultAppealItem> saveAll(List<AssessmentResultAppealItem> items);
    List<AssessmentResultAppealItem> findByAppealId(UUID appealId);
    /** Manual cleanup when an assessment session is deleted — no FK blocks it. */
    void deleteByAppealIdIn(Collection<UUID> appealIds);
}
