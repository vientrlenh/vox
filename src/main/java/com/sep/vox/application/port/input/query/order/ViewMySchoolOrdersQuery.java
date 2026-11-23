package com.sep.vox.application.port.input.query.order;

/**
 * Carries no schoolId: the school is taken from the token of the logged-in school admin, for the same reason as
 * {@link com.sep.vox.application.port.input.command.order.CreateSubscriptionOrderCommand}.
 */
public record ViewMySchoolOrdersQuery(
    int page,
    int size
) {
}
