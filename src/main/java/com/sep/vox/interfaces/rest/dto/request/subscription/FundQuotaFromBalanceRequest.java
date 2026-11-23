package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tops up the quota wallet of one quota type from the school's self top-up wallet.
 *
 * <p>{@code quotaType} is in the path and not here, in the same mould as the endpoint that sets the distribution ceiling.
 */
public record FundQuotaFromBalanceRequest(
    // inclusive = false: 0 VND is not a top-up, and a 0 VND ledger entry directly violates
    // chk_school_balance_entries_quota_funding_traceable (which requires amount_vnd < 0 after the sign flip).
    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Số tiền nạp phải lớn hơn 0")
    BigDecimal amountVnd,

    // Optional: the administrator's note, stored directly in the ledger entry. Matches the column length of
    // school_balance_entries.reason varchar(2048) -- truncated here so the error is a readable message instead of
    // a string truncation error from the driver.
    @Size(max = 2048, message = "Ghi chú không được vượt quá 2048 ký tự")
    String reason
) {
}
