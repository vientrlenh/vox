package com.sep.vox.application.common;

public final class CacheKey {
    
    public static final String RESET_PASSWORD_PREFIX = "reset-password:";
    public static final String EMAIL_PREFIX = "email:";
    public static final String OTP_PREFIX = "otp:";
    public static final String REGISTER_VERIFICATION_PREFIX = "register_verification:";
    public static final String LOCK_PREFIX = "lock:";
    public static final String SCHOOL_DIRECTORY_PREFIX = "school-directory:";
    public static final String FULL_NAME_PREFIX = "full-name:";
    public static final String IDENTITY_NUMBER_PREFIX = "identity-number:";
    public static final String PHONE_PREFIX = "phone:";
    public static final String DOB_PREFIX = "date-of-birth:";
    public static final String ADDRESS_PREFIX = "address:";
    public static final String POSTAL_CODE_PREFIX = "postal-code:";
    public static final String POSITION_PREFIX = "position:";
    public static final String STUDENT_COUNT_PREFIX = "student-count:";

    public static String registerVerificationKey(String email) {
        return REGISTER_VERIFICATION_PREFIX + email;
    }
}
