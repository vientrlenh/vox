package com.sep.vox.interfaces.rest.dto.request.registration;

import com.sep.vox.application.port.input.command.registration.RegisterFromSchoolDirectoryCommand;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterFromSchoolDirectoryRequest(
    @NotNull(message = "School directory must not be blank")
    UUID schoolDirectoryId,

    @NotBlank(message = "Contact name must not be blank")
    @Size(max = 255, message = "Contact name must not exceed 255 characters")
    String contactFullName,

    @NotBlank(message = "Identifier code must not be blank")
    @Size(max = 20, message = "Identifier code must not exceed 20 characters")
    String identityNumber,

    @NotBlank(message = "Contact phone number must not be blank")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String contactPhone,

    @NotBlank(message = "Contact email must not be blank")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    @Email(message = "Email is invalid")
    String contactEmail,

    @NotNull(message = "Date of birth must not be blank")
    LocalDate dateOfBirth,

    @NotBlank(message = "Contact address must not be blank")
    @Size(max = 512, message = "Contact address must not exceed 512 characters")
    String contactAddress,

    @NotBlank(message = "Postal code must not be blank")
    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    String postalCode,

    @NotBlank(message = "Position must not be blank")
    @Size(max = 50, message = "Position must not exceed 50 characters")
    String position,

    @NotNull(message = "Student count must not be blank")
    @Min(value = 1, message = "Student count must not be less than 1")   
    @Max(value = Integer.MAX_VALUE, message = "Student count must not exceed " + Integer.MAX_VALUE)
    Integer studentCount, 

    List<String> documentUrls
) {

    public static RegisterFromSchoolDirectoryCommand toCommand(RegisterFromSchoolDirectoryRequest request) {
        return new RegisterFromSchoolDirectoryCommand(
            request.schoolDirectoryId(),
            request.contactFullName(), 
            request.identityNumber(), 
            request.contactPhone(), 
            request.contactEmail(), 
            request.dateOfBirth(),
            request.contactAddress(),
            request.postalCode(), 
            request.position(), 
            request.studentCount(), 
            request.documentUrls()
        );
    }
}
