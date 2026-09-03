package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record AllocateQuotaRequest(
    @NotBlank(message = "Cách phân bổ không được để trống")
    // Có $ ở cuối: thiếu neo thì regex chỉ đòi chuỗi BẮT ĐẦU bằng AUTO/MANUAL, nên "AUTOMATIC" lọt
    // qua validation rồi chết ở fromString() phía sau -- người dùng nhận 500 thay vì câu báo lỗi này.
    @Pattern(regexp = "^(AUTO|MANUAL)$", message = "Cách phân bổ chỉ chấp nhận giá trị AUTO/MANUAL")
    String mode,

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
