package com.sep.vox.application.response.input.dashboard;

public record SchoolSubscriptionRenewalResponse(
    String planName,
    String status,
    String endDate
) {
    
}
