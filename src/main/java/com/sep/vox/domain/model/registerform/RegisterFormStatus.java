package com.sep.vox.domain.model.registerform;

/*
PENDING_VERIFICATION -> PENDING_REVIEW -> APPROVED/AUTO_APPROVED_REJECTED
 */
public enum RegisterFormStatus {
    PENDING,
    AUTO_APPROVED, 
    APPROVED,
    REJECTED, 
    EXPIRED
}
