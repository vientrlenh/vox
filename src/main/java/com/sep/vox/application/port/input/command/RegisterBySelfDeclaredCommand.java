package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegisterBySelfDeclaredCommand(
    String schoolName, 
    String schoolDomain, 
    UUID schoolWardId, 
    UUID schoolCityId, 
    UUID schoolProvinceId, 
    String schoolStreetAddress, 
    String contactFullName, 
    String identityNumber, 
    String contactPhone, 
    String contactEmail, 
    LocalDate birthDate, 
    String contactAddress, 
    String postalCode, 
    Integer studentCount, 
    List<String> documentUrls
) {
    
}
