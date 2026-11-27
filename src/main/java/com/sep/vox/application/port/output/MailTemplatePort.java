package com.sep.vox.application.port.output;

public interface MailTemplatePort {
    String renderPasswordSetUpEmail(String schoolAdminName, String schoolName, String passwordSetupUrl, String expiresIn);
    String renderSchoolUserPasswordSetUpEmail(String schoolUserName, String schoolName, String passwordSetupUrl, String expiresIn);
    String renderRejectRegisterFormEmail(String reason);
    String renderResetPasswordOtpEmail(String otp, String expiresIn);
    String renderRegisterVerificationOtpEmail(String otp, String expiresIn);
    String renderAppealRejectedEmail(String assessmentName, String reason);
    String renderAppealPublishedEmail(String assessmentName, String scoreBefore, String scoreAfter);

    // ---- score lifecycle notifications ------------------------------------------
    //
    // The seven mails below share ONE template (`result-notification.html`) with
    // title / lead-in / body slots. They differ only in their TEXT, so seven nearly
    // identical HTML files would be seven places to edit on every rebrand or layout change.
    // The wording itself stays in the render layer and does not leak down to the listener.

    /** The score is published for the first time. */
    String renderResultReleasedEmail(String assessmentName, String totalScore);

    /** An ALREADY published score has changed (post-review or appeal). */
    String renderResultRegradedEmail(String assessmentName, String roundLabel, String scoreBefore, String scoreAfter);

    String renderResultInvalidatedEmail(String assessmentName, String reason);

    String renderResultInvalidClearedEmail(String assessmentName, String reason);

    /** An appeal has been approved and a grader has been assigned. */
    String renderAppealApprovedEmail(String assessmentName, String deadline);

    /** The final PASSED/FAILED outcome after the assessment is published. */
    String renderResultOutcomeDecidedEmail(String assessmentName, String outcomeLabel, String totalScore);

    /** Reminds a teacher that a grading assignment is about to be due or is overdue. */
    String renderGradingDeadlineReminderEmail(String assessmentName, String roundLabel, String deadline);

    /** Tells the admin that a teacher has returned a grading assignment. */
    String renderGradingDeclinedEmail(String assessmentName, String teacherName, String reason);

    /** A successful PayOS invoice payment (new subscription / renewal / extra token purchase). */
    String renderInvoicePaidEmail(
        String schoolName,
        String invoiceNumber,
        String itemTitle,
        String amountLabel,
        String paidAtLabel,
        String validUntilLabel
    );

    // ---- AI quota debt (section 13, AI_USAGE_QUOTA_USD_MIGRATION.md) ----------

    /** A school's AI quota debt has just exceeded the warning cap -- sent to every SYSTEM_ADMIN. */
    String renderDebtCapExceededEmail(String schoolName, String quotaLabel, String overageLabel, String capLabel);

    /** The school has just been locked because the real AI cost exceeded its quota -- sent to every SCHOOL_ADMIN of the school. */
    String renderSchoolLockedDueToDebtEmail(String schoolName);

    /** The school's AI quota debt is cleared (extra purchase/renewal/upgrade) -- sent to every SCHOOL_ADMIN of the school. */
    String renderSchoolDebtClearedEmail(String schoolName);

    // ---- subscription suspension / unsuspension -----------------------------

    /** The school's subscription has just been suspended (by a System Admin or an automatic job) -- sent to every SCHOOL_ADMIN of the school. */
    String renderSchoolSubscriptionSuspendedEmail(String schoolName, String reason);

    /** The school's subscription has just been unsuspended -- sent to every SCHOOL_ADMIN of the school. */
    String renderSchoolSubscriptionUnsuspendedEmail(String schoolName);

}
