package com.sep.vox.interfaces.rest.dto.request.school;

import com.sep.vox.application.port.input.command.school.CreateSchoolCommand;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolRequest(
    UUID schoolDirectoryId, 

    @Size(max = 100, message = "School code must not exceed 100 characters")
    String schoolCode, 

    @Size(max = 255, message = "School name must not exceed 255 characters")
    String schoolName, 

    @Size(max = 512, message = "School address must not exceed 512 characters")
    String schoolAddress, 

    @Size(max = 100, message = "School domain must not exceed 100 characters")
    String schoolDomain, 

    @NotNull(message = "Number of students in the school must not be blank")
    @Min(value = 1, message = "Student count must not be less than 1")
    Integer studentCount, 

    @NotBlank(message = "School admin email must not be blank")
    @Size(max = 255, message = "School admin email must not exceed 255 characters")
    String adminEmail, 

    @Size(max = 20, message = "School admin phone number must not exceed 20 characters")
    String adminPhone, 

    @NotBlank(message = "Full name of the school admin must not be blank")
    @Size(max = 255, message = "Full name of the school admin must not exceed 255 characters")
    String adminFullName, 

    @NotNull(message = "Date of birth of the school admin must not be blank")
    LocalDate adminDateOfBirth, 

    @Size(max = 255, message = "Address of the school admin must not exceed 255 characters")
    String adminAddress, 

    @Size(max = 4096, message = "Avatar URL of the school admin must not exceed 4096 characters")
    String adminAvatarUrl
) {

    public static CreateSchoolCommand toCommand(CreateSchoolRequest request) {
        return new CreateSchoolCommand(
            request.schoolDirectoryId(), 
            request.schoolCode(), 
            request.schoolName(), 
            request.schoolAddress(), 
            request.schoolDomain(), 
            request.studentCount(), 
            request.adminEmail(),
            request.adminPhone(),
            request.adminFullName(),
            request.adminDateOfBirth(),
            request.adminAddress(), 
            request.adminAvatarUrl()
        );
    }
}
