package com.sep.vox.domain.model.registerform;

public enum RegisterFormStatus {
    PENDING,
    AUTO_APPROVED, 
    APPROVED,
    REJECTED, 
    EXPIRED; 

    public static RegisterFormStatus from(String status) {
        return status == null ? null : RegisterFormStatus.valueOf(status);
    }

    public static String value(RegisterFormStatus status) {
        return status == null ? null : status.name();
    }
}
