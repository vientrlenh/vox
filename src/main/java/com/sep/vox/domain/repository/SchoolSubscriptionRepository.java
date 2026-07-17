package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.SchoolSubscription;

public interface SchoolSubscriptionRepository {
    Optional<SchoolSubscription> findById(UUID id);
    SchoolSubscription save(SchoolSubscription subscription);
    Optional<SchoolSubscription> findActiveBySchoolId(UUID schoolId);
    List<SchoolSubscription> findAllBySchoolId(UUID schoolId);
}
