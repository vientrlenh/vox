package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.ForceSuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * A System Admin forcibly cuts access IMMEDIATELY -- used when a school is found violating the rules (fraud...),
 * always with a reason.
 *
 * <p>Different from CancelSubscriptionUseCase: cancelling only turns off renewal, and the plan stays ACTIVE and
 * usable until endDate. Suspension cuts access right away.
 *
 * <p>Every suspension writes one school_subscription_events row. The three suspended_* columns on the plan only
 * say "is it CURRENTLY suspended" and are wiped when the suspension is lifted -- they are not history. See
 * SchoolSubscriptionEvent.
 */
@Service
@RequiredArgsConstructor
public class ForceSuspendSubscriptionUseCase implements IUseCase<ForceSuspendSubscriptionCommand, UUID> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionEventRepository schoolSubscriptionEventRepository;
    private final SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(ForceSuspendSubscriptionCommand input) {
        String reason = requireReason(input);
        SchoolSubscription subscription = requireActiveSubscription(input.subscriptionId());

        Instant now = Instant.now();
        requireNoOngoingAssessment(subscription, now);

        UUID actorId = userContextPort.getCurrentAuthenticatedUserId();
        SchoolSubscription saved = suspend(subscription, reason, now, actorId);

        schoolSubscriptionEventRepository.save(SchoolSubscriptionEvent.suspended(
            saved.getSchoolId(), saved.getId(), actorId, reason, now));

        suspensionNotificationService.publishSuspended(saved.getId(), saved.getSchoolId(), reason, now);

        return saved.getId();
    }

    // Permission is already blocked at @PreAuthorize("hasRole('SYSTEM_ADMIN')"). Do not re-check isSystemAdmin() here:
    // a System Admin belongs to no school so there is no "own school" to compare against, and the controller has already
    // answered the role question.
    private String requireReason(ForceSuspendSubscriptionCommand input) {
        String reason = StringNormalization.trimAndCollapseSpaces(input.reason());
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason for suspending the subscription is required.");
        }
        return reason;
    }

    private SchoolSubscription requireActiveSubscription(UUID subscriptionId) {
        SchoolSubscription subscription = schoolSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (subscription.getStatus() != SchoolSubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active subscription can be suspended");
        }
        return subscription;
    }

    // Cutting access mid-exam ruins the work of students sitting in the room -- a fault that is not theirs. Make them
    // wait until the session ends and then suspend: a few hours late, but nobody loses their work.
    private void requireNoOngoingAssessment(SchoolSubscription subscription, Instant now) {
        boolean hasOngoingAssessment = assessmentScheduleRepository.findBySchoolId(subscription.getSchoolId()).stream()
            .anyMatch(schedule -> schedule.isOngoingAt(now));
        if (hasOngoingAssessment) {
            throw new IllegalArgumentException(
                "An assessment schedule is in progress at the school, so it cannot be suspended right now. Please try again after the schedule ends.");
        }
    }

    private SchoolSubscription suspend(SchoolSubscription subscription, String reason, Instant now, UUID actorId) {
        subscription.setStatus(SchoolSubscriptionStatus.SUSPENDED);
        subscription.setSuspendedAt(now);
        subscription.setSuspendedReason(reason);
        subscription.setSuspendedBy(actorId);
        return schoolSubscriptionRepository.save(subscription);
    }
}
