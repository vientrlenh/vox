package com.sep.vox.domain.model.subscription;

public enum SchoolSubscriptionEventType {
    /** A System Admin forces loss of access IMMEDIATELY -- always with a reason. */
    SUSPENDED,
    /** Lifts the suspension, returning the plan to ACTIVE. */
    UNSUSPENDED
}
