package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import com.sep.vox.domain.common.DistributionMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public record AllocateQuotaRequest(
    @NotNull(message = "Cách phân bổ không được để trống")
    DistributionMode mode,

    // Bỏ @NotEmpty, do mode auto không cần list này
    // Vẫn giữ @Valid, dùng cho validate các trường trong record nếu có xuất hiện
    @Valid
    List<AllocateUserQuotaAmountRequest> allocations,

    // Nullable: an old client that does not send this field must be treated as false in the mapper, not as a validation error.
    // true = the administrator has confirmed that the part beyond the pool will eat into the school's self-topped-up wallet (see
    // DistributeQuotaToUsersService.computeManualAmounts / the BusinessRuleViolationException thrown without confirmation).
    Boolean confirmWalletDraw
) {
}
