package com.sep.vox.interfaces.rest.dto.request.registration;

import com.sep.vox.application.port.input.command.registration.RegisterBySelfDeclaredCommand;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterBySelfDeclaredRequest(
    @NotBlank(message = "School name must not be blank")
    @Size(max = 255, message = "School name must not exceed 255 characters")
    String schoolName, 

    @Size(max = 100, message = "School domain must not exceed 100 characters")
    String schoolDomain, 

    @NotBlank(message = "Ward of the school must not be blank")
    @Size(max = 255, message = "Ward of the school must not exceed 255 characters")
    String schoolDistrict, 

    @NotBlank(message = "Province/city of the school must not be blank")
    @Size(max = 255, message = "Province/city of the school must not exceed 255 characters")
    String schoolProvince, 

    @NotBlank(message = "School address must not be blank")
    @Size(max = 512, message = "School address must not exceed 512 characters")
    String schoolAddress, 

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

    @NotNull(message = "Document path must not be blank")
    List<String> documentUrls
) {

    public static RegisterBySelfDeclaredCommand toCommand(RegisterBySelfDeclaredRequest request) {
        return new RegisterBySelfDeclaredCommand(
            request.schoolName(), 
            request.schoolDomain(), 
            request.schoolDistrict(), 
            request.schoolProvince(), request.schoolAddress(), request.contactFullName(), 
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
