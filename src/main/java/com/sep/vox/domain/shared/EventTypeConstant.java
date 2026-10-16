package com.sep.vox.domain.shared;

import java.util.List;

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

    /** Bài thi đóng lại mà còn bài ở PENDING_REVIEW ("Chờ soát điểm AI") -- cần người chấm tay. */
    public static final String ASSESSMENT_HUMAN_GRADING_REQUIRED = "AssessmentHumanGradingRequired";

    /** Giáo viên vừa được giao một vòng chấm cụ thể (chấm lần đầu, hậu kiểm, phúc khảo...). */
    public static final String GRADING_ASSIGNMENT_OPENED = "GradingAssignmentOpened";

    public static final String INVOICE_PAID = "InvoicePaid";

    public static final String SCHOOL_DEBT_CAP_EXCEEDED = "SchoolDebtCapExceeded";
    public static final String SCHOOL_LOCKED_DUE_TO_DEBT = "SchoolLockedDueToDebt";
    public static final String SCHOOL_DEBT_CLEARED = "SchoolDebtCleared";
    public static final String SCHOOL_QUOTA_USAGE_WARNING = "SchoolQuotaUsageWarning";

    public static final String SCHOOL_SUBSCRIPTION_SUSPENDED = "SchoolSubscriptionSuspended";
    public static final String SCHOOL_SUBSCRIPTION_UNSUSPENDED = "SchoolSubscriptionUnsuspended";

    // Hai event OTP dưới đây CỐ TÌNH không mang mã OTP trong payload -- chúng chỉ nói "cần gửi
    // OTP cho địa chỉ này". Mã được sinh tại consumer ngay trước lúc gửi mail, giống hệt token
    // đặt mật khẩu ở UserCreatedEventConsumer. Nhờ vậy credential không bao giờ nằm trong
    // outboxes.payload (có backup) hay trong topic Kafka (giữ theo retention).
    public static final String RESET_PASSWORD_OTP_REQUESTED = "ResetPasswordOtpRequested";
    public static final String REGISTER_VERIFICATION_OTP_REQUESTED = "RegisterVerificationOtpRequested";

    public static List<String> all() {
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
            GRADING_ASSIGNMENT_OPENED,
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
