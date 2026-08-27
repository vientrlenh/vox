package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;


public record AllocateQuotaRequest(
    @NotBlank(message = "Cách phân bổ không được để trống")
    // Có $ ở cuối: thiếu neo thì regex chỉ đòi chuỗi BẮT ĐẦU bằng AUTO/MANUAL, nên "AUTOMATIC" lọt
    // qua validation rồi chết ở fromString() phía sau -- người dùng nhận 500 thay vì câu báo lỗi này.
    @Pattern(regexp = "^(AUTO|MANUAL)$", message = "Cách phân bổ chỉ chấp nhận giá trị AUTO/MANUAL")
    String mode,

    @NotEmpty(message = "Danh sách phân bổ không được để trống")
    @Valid
    List<AllocateUserQuotaAmountRequest> allocations
) {
}
