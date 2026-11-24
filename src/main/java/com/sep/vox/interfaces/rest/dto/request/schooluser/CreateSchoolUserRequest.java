package com.sep.vox.interfaces.rest.dto.request.schooluser;

import com.sep.vox.application.port.input.command.schooluser.CreateSchoolUserCommand;
import java.util.UUID;
import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolUserRequest(
    @NotBlank(message = "Email không được để trống")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @Email(message = "Email không hợp lệ")
    String email,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    String phone,

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    String fullName,

    @NotNull(message = "Ngày sinh không được để trống")
    LocalDate dateOfBirth,

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    String address,

    @NotBlank(message = "Vai trò không được để trống")
    String roleCode,

    LocalDate startDate,

    LocalDate endDate
) {

    public static CreateSchoolUserCommand toCommand(UUID schoolId, CreateSchoolUserRequest request) {
        return new CreateSchoolUserCommand(
            schoolId,
            request.email(),
            request.phone(),
            request.fullName(),
            request.dateOfBirth(),
            request.address(),
            request.roleCode(),
            request.startDate(),
            request.endDate()
        );
    }
}
