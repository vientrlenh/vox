package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.math.BigDecimal;
import java.util.List;

import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanQuotaCommand;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubscriptionPlanRequest(
    @NotBlank(message = "Subscription plan name must not be blank")
    @Size(max = 255, message = "Subscription plan name must not exceed 255 characters")
    String name, 
    
    @NotBlank(message = "Plan description must not be blank")
    @Size(max = 2048, message = "Plan description must not exceed 2048 characters")
    String tagline,

    @NotNull(message = "Price of the subscription plan must not be blank")
    BigDecimal priceVnd,

    @NotNull(message = "Phase type of the subscription plan must not be blank")
    SubscriptionPlanPeriod periodType,

    @NotNull(message = "Duration amount of the subscription plan phase must not be blank")
    @Min(value = 1, message = "Duration amount of the subscription plan phase must not be less than 1")
    Integer periodCount,

    @NotNull(message = "Minutes of a class test must not be blank")
    @Min(value = 1, message = "Minutes of a class test must not be less than 1")
    Integer maxTimePerAttemptMin,

    @NotEmpty(message = "Quota list of the subscription plan must not be blank")
    @Valid
    List<CreateSubscriptionPlanQuotaRequest> quotas
) {

    public static CreateSubscriptionPlanCommand toCommand(CreateSubscriptionPlanRequest request) {
        return new CreateSubscriptionPlanCommand(
            request.name(),
            request.tagline(),
            request.priceVnd(),
            request.periodType(),
            request.periodCount(),
            request.maxTimePerAttemptMin(),
            request.quotas().stream()
                .map(item -> new CreateSubscriptionPlanQuotaCommand(item.quotaType(), item.includedAmountVnd()))
                .toList()
        );
    }
}
