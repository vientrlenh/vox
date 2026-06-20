package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.List;

public record RegisterBySelfDeclaredCommand(
    String schoolName, 
    String schoolDomain, 
    String schoolDistrict, 
    String schoolProvince, 
    String schoolAddress, 
    String contactFullName, 
    String identityNumber, 
    String contactPhone, 
    String contactEmail, 
    LocalDate dateOfBirth, 
    String contactAddress, 
    String postalCode, 
    String position, 
    int studentCount, 
    List<String> documentUrls
) {
    
}
