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

    // Nullable: client cũ chưa gửi field này phải coi như false ở mapper, không phải lỗi validation.
    // true = quản trị viên đã xác nhận phần vượt pool sẽ ăn vào ví tự nạp của trường (xem
    // DistributeQuotaToUsersService.computeManualAmounts / WalletDrawConfirmationRequiredException).
    Boolean confirmWalletDraw
) {
}
