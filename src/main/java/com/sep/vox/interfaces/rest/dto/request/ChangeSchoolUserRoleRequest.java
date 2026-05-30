package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeSchoolUserRoleRequest(
    @NotBlank(message = "Vai trò không được để trống")
    String roleCode
) {

}
