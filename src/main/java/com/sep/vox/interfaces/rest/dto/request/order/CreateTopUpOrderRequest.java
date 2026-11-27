package com.sep.vox.interfaces.rest.dto.request.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * creditAmountVnd is the school balance the school WANTS TO RECEIVE, before the service fee -- the amount to pay is
 * creditAmountVnd + fee, computed by the use case and stored in orders.total_amount_vnd.
 */
public record CreateTopUpOrderRequest(
    @NotNull(message = "Top-up amount must not be blank")
    @DecimalMin(value = "1", message = "Top-up amount must be greater than 0")
    // fraction = 0 because VND has no minor unit and PayOS/SePay reject decimal amounts; integer = 15 matches
    // orders.subtotal_amount_vnd numeric(15,0). Rejected here so the caller gets a readable 400 instead of the use case throwing
    // IllegalArgumentException or Postgres silently rounding.
    @Digits(integer = 15, fraction = 0, message = "Top-up amount must be a whole VND number with at most 15 digits")
    BigDecimal creditAmountVnd
) {
}
