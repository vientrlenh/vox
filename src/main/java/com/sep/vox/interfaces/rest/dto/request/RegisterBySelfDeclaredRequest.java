package com.sep.vox.interfaces.rest.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.RegisterBySelfDeclaredCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record RegisterBySelfDeclaredRequest(
    @NotBlank(message = "School name is required")
    @Size(max = 255, message = "School name must not exceed 255 characters")
    String schoolName, 

    @Size(max = 100, message = "School domain must not exceed 100 characters")
    String schoolDomain, 

    @NotNull(message = "School ward ID is required")
    UUID schoolWardId, 

    UUID schoolCityId, 

    @NotNull(message = "School province ID is required")
    UUID schoolProvinceId,

    @NotBlank(message = "School street address is required")
    @Size(max = 512, message = "School street address must not exceed 512 characters")
    String schoolStreetAddress, 

    @NotBlank(message = "Contact full name is required")
    @Size(max = 255, message = "Contact full name must not exceed 255 characters")
    String contactFullName, 

    @NotBlank(message = "Identity number is required")
    @Size(max = 20, message = "Identity number must not exceed 20 characters")    
    String identityNumber, 

    @NotBlank(message = "Contact phone is required")
    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    String contactPhone,
    
    @NotBlank(message = "Contact email is required")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    @Email(message = "Invalid email address")
    String contactEmail, 

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    LocalDate birthDate, 

    @NotBlank(message = "Contact address is required")
    @Size(max = 512, message = "Contact address must not exceed 512 characters")
    String contactAddress,
    
    @NotBlank(message = "Postal code is required")
    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    String postalCode, 

    @NotNull(message = "Student count is required")
    @Min(value = 1, message = "Student count must not lower than 1")   
    @Max(value = Integer.MAX_VALUE, message = "Student count must not greater than " + Integer.MAX_VALUE)
    Integer studentCount, 

    @NotNull(message = "Document urls are required")
    List<String> documentUrls
) {
    
    public static RegisterBySelfDeclaredCommand toCommand(RegisterBySelfDeclaredRequest request) {
        return new RegisterBySelfDeclaredCommand(
            request.schoolName, 
            request.schoolDomain,  
            request.schoolWardId, 
            request.schoolCityId, 
            request.schoolProvinceId, 
            request.schoolStreetAddress, 
            request.contactFullName, 
            request.identityNumber, 
            request.contactPhone, 
            request.contactEmail, 
            request.birthDate, 
            request.contactAddress, 
            request.postalCode, 
            request.studentCount, 
            request.documentUrls
        );
    }
}
