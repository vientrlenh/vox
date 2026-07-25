package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;

public interface SubscriptionRequestRepository {
    Optional<SubscriptionRequest> findById(UUID id);
    SubscriptionRequest save(SubscriptionRequest request);
    List<SubscriptionRequest> findAllBySchoolId(UUID schoolId);
    List<SubscriptionRequest> findAllByStatus(RequestStatus status);
}
