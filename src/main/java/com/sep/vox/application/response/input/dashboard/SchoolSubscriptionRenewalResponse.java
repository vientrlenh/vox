package com.sep.vox.application.response.input.dashboard;

import java.time.Instant;

public record SchoolSubscriptionRenewalResponse(
    String planName,
    String status,
    Instant endDate
) {
    
}
