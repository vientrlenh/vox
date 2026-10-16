package com.sep.vox.infrastructure.properties;

import java.time.LocalDate;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.shared.StringNormalization;

@ConfigurationProperties(prefix = "app.initializer.system-admin")
public record SystemAdminInitializeProperties(
    String email, 
    String password, 
    String phone, 
    String fullName, 
    LocalDate birthDate, 
    String address
) {
    public SystemAdminInitializeProperties {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("System admin initialize email is not configured");
        }
        if (password == null) {
            throw new IllegalStateException("System admin initialize password is not configured");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalStateException("System admin initialize full name is not configured");
        }
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("System admin initialize birth date is not configured or is after today");
        }
        email = StringNormalization.normalizeEmail(email);
        phone = StringNormalization.normalizePhone(phone);
        fullName = StringNormalization.trimAndCollapseSpaces(fullName);
        address = StringNormalization.trimAndCollapseSpaces(address);
    }
}
