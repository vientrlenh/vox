package com.sep.vox.application.port.input.command.payment;

import java.util.UUID;

// acceptedPlanId: must match the plan that PreviewRenewalUseCase returned IF AND ONLY IF the subscription's
// current plan has been archived and has a replacement plan (renewalPlan != current plan) -- that is, the
// school must preview and confirm the new plan before the system creates the payment link. Null if the plan
// is unchanged (nothing more to confirm).
public record CreatePaymentLinkForRenewalCommand(
    UUID schoolId,
    UUID subscriptionId,
    UUID acceptedPlanId, 
    String paymentMethod
) {
}