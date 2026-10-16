package com.sep.vox.interfaces.rest.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record CreateSchoolRequest(
    UUID schoolDirectoryId, 

    @Size(max = 100, message = "School code must not exceed 100 characters")
    String schoolCode, 

    @Size(max = 255, message = "School name must not exceed 255 characters")
    String schoolName, 

    UUID schoolWardId, 

    UUID schoolCityId, 

    UUID schoolProvinceId,

    @Size(max = 512, message = "School street address must not exceed 512 characters")
    String schoolStreetAddress, 

    @Size(max = 100, message = "School domain must not exceed 100 characters")
    String schoolDomain, 

    @NotNull(message = "Number of school student is required")
    @Min(value = 1, message = "Number of student must not lower than 1")
    @Max(value = Integer.MAX_VALUE, message = "Number of student must not greater than " + Integer.MAX_VALUE)
    Integer studentCount, 

    @NotBlank(message = "School admin email is required")
    @Size(max = 255, message = "School admin email must not exceed 255 characters")
    String adminEmail, 

    @Size(max = 20, message = "School admin phone number must not exceed 20 characters")
    String adminPhone, 

    @NotBlank(message = "School admin full name is required")
    @Size(max = 255, message = "School admin full name must not exceed 255 characters")
    String adminFullName, 

    @NotNull(message = "School admin birth date is required")
    @Past(message = "School admin birth date must be in the past")
    LocalDate adminBirthDate, 

    @Size(max = 512, message = "School admin address must not exceed 512 characters")
    String adminAddress, 

    @Size(max = 4096, message = "School admin avatar url must not exceed 4096 characters")
    String adminAvatarUrl
) {
    
}
