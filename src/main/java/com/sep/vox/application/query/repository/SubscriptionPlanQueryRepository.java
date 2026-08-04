package com.sep.vox.application.query.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.PlanStatus;

public interface SubscriptionPlanQueryRepository {
    Page<SubscriptionPlanDto> findAllByStatus(PlanStatus status, Pageable pageable);
    Page<SubscriptionPlanDto> findAll(Pageable pageable);
}
