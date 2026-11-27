package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.UnsuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Lifts a suspension and returns the plan to ACTIVE.
 *
 * <p>Does NOT extend endDate to make up for the suspended time. This is a business decision, not an omission:
 * suspension is a sanction on a school that broke the rules, and adding the time back would turn it into a
 * punishment that costs nothing. For a mistaken suspension the remedy is a System Admin adjusting by hand, with
 * an actor and a reason, not an automatic rule applied to schools that really did violate.
 *
 * <p>The three suspended_* columns are cleared to null here -- and that is exactly why the history must be
 * written to school_subscription_events first, otherwise after this command nothing would show that the school
 * was ever suspended.
 */
@Service
@RequiredArgsConstructor
public class UnsuspendSubscriptionUseCase implements IUseCase<UnsuspendSubscriptionCommand, UUID> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionEventRepository schoolSubscriptionEventRepository;
    private final SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(UnsuspendSubscriptionCommand input) {
        SchoolSubscription subscription = requireSuspended(input.subscriptionId());

        Instant now = Instant.now();
        UUID actorId = userContextPort.getCurrentAuthenticatedUserId();
        // The note is optional: lifting a suspension gives rights back rather than taking them away, so it does not need
        // justifying the way suspending does. The DB CHECK also only forces a reason for SUSPENDED.
        String note = StringNormalization.trimAndCollapseSpaces(input.note());

        // Write the ledger BEFORE clearing the three suspended_* columns: after the save below the suspension reason is
        // nowhere left to copy from.
        schoolSubscriptionEventRepository.save(SchoolSubscriptionEvent.unsuspended(
            subscription.getSchoolId(), subscription.getId(), actorId, note, now));

        SchoolSubscription saved = clearSuspension(subscription);

        suspensionNotificationService.publishUnsuspended(saved.getId(), saved.getSchoolId(), now);

        return saved.getId();
    }

    private SchoolSubscription requireSuspended(UUID subscriptionId) {
        SchoolSubscription subscription = schoolSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (subscription.getStatus() != SchoolSubscriptionStatus.SUSPENDED) {
            throw new IllegalArgumentException("This subscription is not currently suspended");
        }
        return subscription;
    }

    private SchoolSubscription clearSuspension(SchoolSubscription subscription) {
        subscription.setStatus(SchoolSubscriptionStatus.ACTIVE);
        subscription.setSuspendedAt(null);
        subscription.setSuspendedReason(null);
        subscription.setSuspendedBy(null);
        return schoolSubscriptionRepository.save(subscription);
    }
}
