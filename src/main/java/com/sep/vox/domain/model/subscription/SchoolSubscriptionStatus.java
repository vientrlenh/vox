package com.sep.vox.domain.model.subscription;

public enum SchoolSubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED,
    // A System Admin forces loss of access IMMEDIATELY (unlike CANCELLED, which only turns off renewal and stays ACTIVE
    // until endDate) -- see ForceSuspendSubscriptionUseCase.
    SUSPENDED
}
