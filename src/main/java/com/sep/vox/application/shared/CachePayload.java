package com.sep.vox.application.shared;

import java.time.LocalDate;
import java.util.UUID;

public final class CachePayload {
    public record RegisterVerificationPayload(
        String otpHash,
        String email, 
        UUID schoolDirectoryId, 
        String fullName, 
        String identityNumber, 
        String phone, 
        LocalDate dateOfBirth, 
        String address, 
        String postalCode, 
        String position, 
        int studentCount
    ) {
        
    }
}
