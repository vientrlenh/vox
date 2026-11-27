package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class AssessmentTimeQuotaGuardService {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public void requireWithinPlan(UUID schoolId, Integer durationSeconds, String targetLabel) {
        if (schoolId == null || durationSeconds == null || durationSeconds <= 0) {
            return;
        }

        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new BusinessRuleViolationException(
                "The school has no active subscription, so this assessment blueprint cannot be attached."));
        SubscriptionPlan plan = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        Integer maxMinutes = plan.getMaxTimePerAttemptMin();
        if (maxMinutes == null || maxMinutes <= 0) {
            return;
        }

        long maxSeconds = maxMinutes.longValue() * 60L;
        if (durationSeconds.longValue() > maxSeconds) {
            throw new BusinessRuleViolationException(
                "%s has a duration of %s, which exceeds the %s limit of the %s plan. Please reduce the questions or upgrade the plan before attaching."
                    .formatted(targetLabel, formatDuration(durationSeconds), formatDuration(maxSeconds), plan.getName()));
        }
    }

    private String formatDuration(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long minutes = safeSeconds / 60L;
        long remainingSeconds = safeSeconds % 60L;
        if (remainingSeconds == 0L) {
            return minutes + " minutes";
        }
        return minutes + " minutes " + remainingSeconds + " seconds";
    }
}
