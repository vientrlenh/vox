package com.sep.vox.application.port.input.command.order;

import java.util.UUID;

/**
 * Carries no schoolId: the school is TAKEN FROM THE TOKEN of the logged-in school admin. If it came from the payload,
 * @PreAuthorize("hasRole('SCHOOL_ADMIN')") would not be enough protection -- it only answers "is this a school admin",
 * not "is this a school admin OF THIS SCHOOL", so an admin of school A could still place an order
 * for school B. Derived from the token there is nothing to check because there is nothing to declare falsely.
 *
 * <p>It also does not carry requestType/currentPlanId like the old SubmitRequestCommand: both can be derived from the school's
 * ACTIVE plan, and a derived value never disagrees with the truth.
 */
public record CreateSubscriptionOrderCommand(
    UUID subscriptionPlanId
) {
}
