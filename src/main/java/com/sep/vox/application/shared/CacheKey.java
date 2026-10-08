package com.sep.vox.application.shared;

import java.util.UUID;

public final class CacheKey {
    
    private static final String RESET_PASSWORD_PREFIX = "reset-password:";
    private static final String OTP_PREFIX = "otp:";
    private static final String REGISTER_VERIFICATION_PREFIX = "register_verification:";
    private static final String EXAM_SCHEDULE_PREFIX = "exam-schedule:";

    public static String resetPasswordKey(String email) {
        return RESET_PASSWORD_PREFIX + OTP_PREFIX + email;
    }

    public static String registerVerificationKey(String email) {
        return REGISTER_VERIFICATION_PREFIX + email;
    }

    public static String examScheduleOtpKey(UUID scheduleId) {
        return EXAM_SCHEDULE_PREFIX + OTP_PREFIX + scheduleId.toString();
    }
}
