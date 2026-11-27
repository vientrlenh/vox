package com.sep.vox.domain.common;

import java.util.List;
import java.util.stream.Stream;

public final class EventTypeConstant {

    private EventTypeConstant() {}
    
    public static final String REGISTER_FORM_REJECTED = "RegisterFormRejected";
    public static final String USER_CREATED = "UserCreated";

    public static final String ASSESSMENT_APPEAL_PUBLISHED = "AssessmentAppealPublished";
    public static final String ASSESSMENT_APPEAL_REJECTED = "AssessmentAppealRejected";
    public static final String ASSESSMENT_APPEAL_APPROVED = "AssessmentAppealApproved";

    public static final String ASSESSMENT_RESULT_RELEASED = "AssessmentResultReleased";
    public static final String ASSESSMENT_RESULT_REGRADED = "AssessmentResultRegraded";
    public static final String ASSESSMENT_RESULT_INVALIDATED = "AssessmentResultInvalidated";
    public static final String ASSESSMENT_RESULT_INVALID_CLEARED = "AssessmentResultInvalidCleared";
    public static final String ASSESSMENT_RESULT_OUTCOME_DECIDED = "AssessmentResultOutcomeDecided";

    public static final String GRADING_DEADLINE_REMINDER = "GradingDeadlineReminder";
    public static final String GRADING_ASSIGNMENT_DECLINED = "GradingAssignmentDeclined";
    public static final String ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED = "AssessmentBlueprintVersionPublished";

    /** An assessment closed while a session is still in PENDING_REVIEW ("Awaiting AI score review") -- needs a manual grader. */
    public static final String ASSESSMENT_HUMAN_GRADING_REQUIRED = "AssessmentHumanGradingRequired";

    /** A teacher was just given a specific grading round (first grading, spot check, appeal...). */
    public static final String GRADING_ASSIGNMENT_OPENED = "GradingAssignmentOpened";

    public static final String INVOICE_PAID = "InvoicePaid";

    public static final String SCHOOL_DEBT_CAP_EXCEEDED = "SchoolDebtCapExceeded";
    public static final String SCHOOL_LOCKED_DUE_TO_DEBT = "SchoolLockedDueToDebt";
    public static final String SCHOOL_DEBT_CLEARED = "SchoolDebtCleared";
    public static final String SCHOOL_QUOTA_USAGE_WARNING = "SchoolQuotaUsageWarning";

    public static final String SCHOOL_SUBSCRIPTION_SUSPENDED = "SchoolSubscriptionSuspended";
    public static final String SCHOOL_SUBSCRIPTION_UNSUSPENDED = "SchoolSubscriptionUnsuspended";

    // The two OTP events below DELIBERATELY do not carry the OTP code in the payload -- they only say "an OTP needs to
    // be sent to this address". The code is generated at the consumer right before the mail is sent, exactly like the
    // password-setting token in UserCreatedEventConsumer. That way the credential never sits in
    // outboxes.payload (which is backed up) or in the Kafka topic (kept per retention).
    public static final String RESET_PASSWORD_OTP_REQUESTED = "ResetPasswordOtpRequested";
    public static final String REGISTER_VERIFICATION_OTP_REQUESTED = "RegisterVerificationOtpRequested";

    public static List<String> all() {
        return Stream.concat(registrationAndAssessmentTypes().stream(), billingSchoolAndOtpTypes().stream())
            .toList();
    }

    private static List<String> registrationAndAssessmentTypes() {
        return List.of(
            REGISTER_FORM_REJECTED,
            USER_CREATED,
            ASSESSMENT_APPEAL_PUBLISHED,
            ASSESSMENT_APPEAL_REJECTED,
            ASSESSMENT_APPEAL_APPROVED,
            ASSESSMENT_RESULT_RELEASED,
            ASSESSMENT_RESULT_REGRADED,
            ASSESSMENT_RESULT_INVALIDATED,
            ASSESSMENT_RESULT_INVALID_CLEARED,
            ASSESSMENT_RESULT_OUTCOME_DECIDED,
            GRADING_DEADLINE_REMINDER,
            GRADING_ASSIGNMENT_DECLINED,
            ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED,
            ASSESSMENT_HUMAN_GRADING_REQUIRED,
            GRADING_ASSIGNMENT_OPENED
        );
    }

    private static List<String> billingSchoolAndOtpTypes() {
        return List.of(
            INVOICE_PAID,
            SCHOOL_DEBT_CAP_EXCEEDED,
            SCHOOL_LOCKED_DUE_TO_DEBT,
            SCHOOL_DEBT_CLEARED,
            SCHOOL_QUOTA_USAGE_WARNING,
            SCHOOL_SUBSCRIPTION_SUSPENDED,
            SCHOOL_SUBSCRIPTION_UNSUSPENDED,
            RESET_PASSWORD_OTP_REQUESTED,
            REGISTER_VERIFICATION_OTP_REQUESTED
        );
    }
}
