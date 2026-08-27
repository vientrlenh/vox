package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;


public record AllocateQuotaRequest(
    @NotBlank(message = "Cách phân bổ không được để trống")
    @Pattern(regexp = "^(AUTO|MANUAL)", message = "Cách phân bổ chỉ chấp nhận giá trị AUTO/MANUAL") 
    String mode,

    @NotEmpty(message = "Danh sách phân bổ không được để trống")
    @Valid
    List<AllocateUserQuotaAmountRequest> allocations
) {
}
