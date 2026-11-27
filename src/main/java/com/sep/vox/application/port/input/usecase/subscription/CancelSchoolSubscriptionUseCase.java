package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * The school announces that it will NOT buy again after the current period ends.
 *
 * <p>This action does NOT cut off access and does NOT refund: the school paid for the whole period, so it keeps
 * using it normally until endDate, and then SubscriptionExpiryJob moves it to EXPIRED like any other period. To
 * cut access immediately that is a suspension (ForceSuspendSubscriptionUseCase), which is a System Admin right.
 *
 * <p>IMPORTANT -- {@code cancelledAt} here is a RECORD OF INTENT, not a switch that controls anything. The system
 * has NO auto-renewal: to get a new period the school has to place an order itself
 * (RenewSchoolSubscriptionUseCase). So "cancel" here stops no charge at all -- there is no charge about to
 * happen on its own anyway. Its value is as a signal: for the FE to show the right status, and for the sales
 * side to learn which schools plan to leave while there is still time to keep them. Do not write any logic that
 * relies on this column on the assumption that it blocks renewal.
 *
 * <p>There is no way to undo a cancellation. If one is added later, {@code cancelledAt} would be cleared back to
 * null and it would no longer be history -- another school_subscription_events row would have to be written, as
 * suspension does. For now this column is written once and never cleared, so it is history by itself.
 */
@Service
@RequiredArgsConstructor
public class CancelSchoolSubscriptionUseCase implements IUseCase<Void, UUID> {

    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(Void input) {
        // Does NOT accept subscriptionId: a school has exactly one period in force, and accepting an id from outside means
        // hasRole('SCHOOL_ADMIN') cannot answer "does this period belong to that school" -- yet another ownership-check
        // layer would have to be built. With no id there is nothing to check.
        UUID schoolId = userContextPort.getCurrentSchoolId();

        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("The school has no active subscription to cancel."));

        if (subscription.getCancelledAt() != null) {
            throw new IllegalArgumentException("The subscription was already cancelled.");
        }

        Instant now = Instant.now();
        requireNoQueuedPeriod(schoolId, now, subscription.getId());

        subscription.setCancelledAt(now);
        return schoolSubscriptionRepository.save(subscription).getId();
    }

    // Refuse when a paid period is queued. Marking "will not renew" while a renewal HAS been paid and is waiting to run is
    // a self-contradicting state: a subscription carrying the cancelled flag would still quietly open a new period. The
    // system has no refund flow either, so it cannot cancel that period on the school's behalf.
    private void requireNoQueuedPeriod(UUID schoolId, Instant now, UUID inForceId) {
        SchoolSubscription queued = queuedPeriod(schoolId, now, inForceId);
        if (queued != null) {
            throw new IllegalArgumentException(
                "The school has paid for a following period (running until " + DISPLAY_DATE.format(queued.getEndDate())
                    + "), so it cannot be marked as cancelled. Please contact support if you want to stop early.");
        }
    }

    /** A period that has not ended and is NOT the running one -- i.e. a period that is paid for and waiting for its start date. */
    private SchoolSubscription queuedPeriod(UUID schoolId, Instant now, UUID inForceId) {
        return schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).stream()
            .filter(period -> !period.getId().equals(inForceId))
            .findFirst()
            .orElse(null);
    }
}
