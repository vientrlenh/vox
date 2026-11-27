package com.sep.vox.interfaces.rest.dto.request.schooluser;

import com.sep.vox.application.port.input.command.schooluser.CreateSchoolUserCommand;
import java.util.UUID;
import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolUserRequest(
    @NotBlank(message = "Email must not be blank")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Email(message = "Email is invalid")
    String email,

    @NotBlank(message = "Phone number must not be blank")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phone,

    @NotBlank(message = "Full name must not be blank")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    String fullName,

    @NotNull(message = "Date of birth must not be blank")
    LocalDate dateOfBirth,

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String address,

    @NotBlank(message = "Role must not be blank")
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
