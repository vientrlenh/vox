package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.subscription.SubscriptionStatus;

public record ViewSchoolSubscriptionsQuery(
    String keyword,
    UUID planId,
    SubscriptionStatus status,
    int page,
    int size
) {
}
