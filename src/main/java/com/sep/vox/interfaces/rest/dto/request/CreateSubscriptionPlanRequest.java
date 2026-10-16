package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSubscriptionPlanRequest(
    @NotBlank(message = "Subscription plan name is required")
    @Size(max = 255, message = "Subscription plan name must not exceed 255 characters")
    String name, 
    
    @NotBlank(message = "Subscription plan tagline is required")
    @Size(max = 2048, message = "Subscription plan tagline must not exceed 2048 characters")
    String tagline,

    @NotNull(message = "Subscription plan price VND is required")
    @DecimalMin(value = "0.0", message = "Subscription plan price VND must not be lower than 0.0 VND")
    BigDecimal priceVnd,

    @NotBlank(message = "Subscription plan period type is required")
    @Pattern(regexp = "^(DAY|MONTH|YEAR)$", message = "Subscription plan period type only accepts DAY/MONTH/YEAR")
    String periodType,

    @NotNull(message = "Subscription plan period count is required")
    @Min(value = 1, message = "Subscription plan period count must not be lower than 1")
    Integer periodCount,

    @NotNull(message = "Subscription plan max time per attempt min is required")
    @Min(value = 1, message = "Subscription plan max time per attempt min must not be lower than 1")
    Integer maxTimePerAttemptMin,

    @NotEmpty(message = "Subscription plan quotas is required")
    @Valid
    List<CreateSubscriptionPlanQuotaRequest> quotas
) {

    
}
