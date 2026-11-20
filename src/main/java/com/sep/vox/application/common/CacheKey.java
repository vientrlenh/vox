package com.sep.vox.application.common;

import java.util.UUID;

public final class CacheKey {
    
    public static final String RESET_PASSWORD_PREFIX = "reset-password:";
    public static final String EMAIL_PREFIX = "email:";
    public static final String OTP_PREFIX = "otp:";
    public static final String REGISTER_VERIFICATION_PREFIX = "register_verification:";
    public static final String EXAM_SCHEDULE_PREFIX = "exam-schedule:";


    public static String registerVerificationKey(String email) {
        return REGISTER_VERIFICATION_PREFIX + email;
    }

    public static String examScheduleOtpKey(UUID scheduleId) {
        return EXAM_SCHEDULE_PREFIX + OTP_PREFIX + scheduleId.toString();
    }
}
