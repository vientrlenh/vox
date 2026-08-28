package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;

public record ViewSchoolSubscriptionsQuery(
    String keyword,
    UUID planId,
    SchoolSubscriptionStatus status,
    int page,
    int size
) {
}
