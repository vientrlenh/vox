package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;

public interface SchoolSubscriptionRepository {
    Optional<SchoolSubscription> findById(UUID id);
    SchoolSubscription save(SchoolSubscription subscription);
    Optional<SchoolSubscription> findActiveBySchoolId(UUID schoolId);
    List<SchoolSubscription> findAllBySchoolId(UUID schoolId);
    PageResult<SchoolSubscription> findAllForAdmin(UUID planId, SubscriptionStatus status, String keyword, int page, int size);
}
