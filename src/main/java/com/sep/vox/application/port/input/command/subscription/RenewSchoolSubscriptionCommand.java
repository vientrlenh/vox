package com.sep.vox.application.port.input.command.subscription;

import java.util.UUID;

/**
 * Has NEITHER schoolId NOR subscriptionId: both are derived from the token, so there is no path to renew on behalf of another
 * school. See RenewSchoolSubscriptionUseCase.
 *
 * @param acceptedPlanId the plan the school HAS SEEN and agreed to on the preview screen. Required, even when
 *                       the plan is unchanged: it is the proof that the school knows what it is buying, and it is what
 *                       blocks the case where the plan is replaced while the school is still looking at the earlier screen.
 */
public record RenewSchoolSubscriptionCommand(
    UUID acceptedPlanId
) {
}
