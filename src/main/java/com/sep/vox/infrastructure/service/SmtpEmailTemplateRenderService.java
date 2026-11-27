package com.sep.vox.infrastructure.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.MailTemplatePort;

@Service
public class SmtpEmailTemplateRenderService implements MailTemplatePort {

    private static final String PASSWORD_SETUP_TEMPLATE =
        "templates/email/register-form-approved-password-setup.html";
    private static final String REGISTER_FORM_REJECTED_TEMPLATE =
        "templates/email/register-form-rejected.html";
    private static final String RESET_PASSWORD_OTP_TEMPLATE =
        "templates/email/reset-password-otp.html";
    private static final String SCHOOL_USER_PASSWORD_SETUP_TEMPLATE =
        "templates/email/school-user-password-setup.html";
    private static final String REGISTER_VERIFICATION_OTP_TEMPLATE =
        "templates/email/register-verification-otp.html";
    private static final String APPEAL_REJECTED_TEMPLATE =
        "templates/email/appeal-rejected.html";
    private static final String APPEAL_PUBLISHED_TEMPLATE =
        "templates/email/appeal-published.html";
    /** Shared frame for the score lifecycle notifications — see the javadoc in MailTemplatePort. */
    private static final String RESULT_NOTIFICATION_TEMPLATE =
        "templates/email/result-notification.html";
    private static final String INVOICE_PAID_TEMPLATE =
        "templates/email/invoice-paid.html";

    @Override
    public String renderPasswordSetUpEmail(String schoolAdminName, String schoolName, String passwordSetupUrl,
            String expiresIn) {
        return loadTemplate(PASSWORD_SETUP_TEMPLATE)
            .replace("{{schoolAdminName}}", escapeHtml(schoolAdminName))
            .replace("{{schoolName}}", escapeHtml(schoolName))
            .replace("{{passwordSetupUrl}}", escapeHtml(passwordSetupUrl))
            .replace("{{expiresIn}}", escapeHtml(expiresIn));
    }

    @Override
    public String renderSchoolUserPasswordSetUpEmail(String schoolUserName, String schoolName,
            String passwordSetupUrl, String expiresIn) {
        return loadTemplate(SCHOOL_USER_PASSWORD_SETUP_TEMPLATE)
            .replace("{{schoolUserName}}", escapeHtml(schoolUserName))
            .replace("{{schoolName}}", escapeHtml(schoolName))
            .replace("{{passwordSetupUrl}}", escapeHtml(passwordSetupUrl))
            .replace("{{expiresIn}}", escapeHtml(expiresIn));
    }

    @Override
    public String renderRejectRegisterFormEmail(String reason) {
        return loadTemplate(REGISTER_FORM_REJECTED_TEMPLATE)
            .replace("{{reason}}", escapeHtml(reason));
    }

    @Override
    public String renderResetPasswordOtpEmail(String otp, String expiresIn) {
        return loadTemplate(RESET_PASSWORD_OTP_TEMPLATE)
            .replace("{{otp}}", escapeHtml(otp))
            .replace("{{expiresIn}}", escapeHtml(expiresIn));
    }

    @Override
    public String renderRegisterVerificationOtpEmail(String otp, String expiresIn) {
        return loadTemplate(REGISTER_VERIFICATION_OTP_TEMPLATE)
            .replace("{{otp}}", escapeHtml(otp))
            .replace("{{expiresIn}}", escapeHtml(expiresIn));
    }


    @Override
    public String renderAppealRejectedEmail(String assessmentName, String reason) {
        return loadTemplate(APPEAL_REJECTED_TEMPLATE)
            .replace("{{assessmentName}}", escapeHtml(assessmentName))
            .replace("{{reason}}", escapeHtml(reason));
    }

    @Override
    public String renderAppealPublishedEmail(String assessmentName, String scoreBefore, String scoreAfter) {
        return loadTemplate(APPEAL_PUBLISHED_TEMPLATE)
            .replace("{{assessmentName}}", escapeHtml(assessmentName))
            .replace("{{scoreBefore}}", escapeHtml(scoreBefore))
            .replace("{{scoreAfter}}", escapeHtml(scoreAfter));
    }

    // ---- score lifecycle notifications ------------------------------------------

    @Override
    public String renderResultReleasedEmail(String assessmentName, String totalScore) {
        return renderNotification(
            "Your assessment score is available",
            "Your submission for the assessment <strong>" + escapeHtml(assessmentName) + "</strong> has been graded.",
            "Result",
            "Total score: <strong>" + escapeHtml(totalScore) + "</strong>");
    }

    @Override
    public String renderResultRegradedEmail(
            String assessmentName, String roundLabel, String scoreBefore, String scoreAfter) {
        return renderNotification(
            "Your assessment score has changed",
            "Your submission for the assessment <strong>" + escapeHtml(assessmentName)
                + "</strong> has just been " + escapeHtml(roundLabel) + ".",
            "Score after regrading",
            "Previous score: " + escapeHtml(scoreBefore)
                + "<br><strong>New score: " + escapeHtml(scoreAfter) + "</strong>");
    }

    @Override
    public String renderResultInvalidatedEmail(String assessmentName, String reason) {
        return renderNotification(
            "Your submission has been invalidated",
            "Your submission for the assessment <strong>" + escapeHtml(assessmentName)
                + "</strong> has been found in violation of the assessment rules.",
            "Reason",
            escapeHtml(reason));
    }

    @Override
    public String renderResultInvalidClearedEmail(String assessmentName, String reason) {
        return renderNotification(
            "Your submission has been reinstated",
            "After review, your submission for the assessment <strong>" + escapeHtml(assessmentName)
                + "</strong> is not considered a violation and will be graded again.",
            "Reason",
            escapeHtml(reason));
    }

    @Override
    public String renderAppealApprovedEmail(String assessmentName, String deadline) {
        return renderNotification(
            "Your appeal has been approved",
            "Your appeal for the assessment <strong>" + escapeHtml(assessmentName)
                + "</strong> has been approved and is being regraded.",
            "Processing deadline",
            escapeHtml(deadline));
    }

    @Override
    public String renderResultOutcomeDecidedEmail(String assessmentName, String outcomeLabel, String totalScore) {
        return renderNotification(
            "Your final result",
            "The result for the assessment <strong>" + escapeHtml(assessmentName) + "</strong> is final.",
            "Result",
            "Classification: <strong>" + escapeHtml(outcomeLabel) + "</strong>"
                + "<br>Total score: " + escapeHtml(totalScore));
    }

    @Override
    public String renderGradingDeadlineReminderEmail(String assessmentName, String roundLabel, String deadline) {
        return renderNotification(
            "Grading deadline reminder",
            "You still have a submission left to grade in the assessment <strong>" + escapeHtml(assessmentName) + "</strong>.",
            "Details",
            "Grading round: " + escapeHtml(roundLabel) + "<br><strong>Deadline: " + escapeHtml(deadline) + "</strong>");
    }

    @Override
    public String renderGradingDeclinedEmail(String assessmentName, String teacherName, String reason) {
        return renderNotification(
            "A teacher returned a grading assignment",
            "An assignment in the assessment <strong>" + escapeHtml(assessmentName)
                + "</strong> was just returned and is waiting to be assigned to someone else.",
            "Details",
            "Teacher: " + escapeHtml(teacherName) + "<br>Reason: " + escapeHtml(reason));
    }

    @Override
    public String renderInvoicePaidEmail(
            String schoolName, String invoiceNumber, String itemTitle,
            String amountLabel, String paidAtLabel, String validUntilLabel) {
        return loadTemplate(INVOICE_PAID_TEMPLATE)
            .replace("{{schoolName}}", escapeHtml(schoolName))
            .replace("{{invoiceNumber}}", escapeHtml(invoiceNumber))
            .replace("{{itemTitle}}", escapeHtml(itemTitle))
            .replace("{{amountLabel}}", escapeHtml(amountLabel))
            .replace("{{paidAtLabel}}", escapeHtml(paidAtLabel))
            .replace("{{validUntilLabel}}", escapeHtml(validUntilLabel));
    }

    // ---- AI quota debt ------------------------------------------------

    @Override
    public String renderDebtCapExceededEmail(String schoolName, String quotaLabel, String overageLabel, String capLabel) {
        return renderNotification(
            "Warning: AI quota debt exceeds the cap",
            "The actual AI cost of the school <strong>" + escapeHtml(schoolName) + "</strong> exceeds the allocated "
                + "quota and now also exceeds the warning cap.",
            "Details",
            "Quota: " + escapeHtml(quotaLabel) + "<br>Debt: <strong>" + escapeHtml(overageLabel)
                + "</strong><br>Warning cap: " + escapeHtml(capLabel));
    }

    @Override
    public String renderSchoolLockedDueToDebtEmail(String schoolName) {
        return renderNotification(
            "The school is locked due to AI quota debt",
            "The actual AI cost of the school <strong>" + escapeHtml(schoolName)
                + "</strong> has exceeded the allocated quota.",
            "What to do",
            "Please pay, or renew or upgrade the plan, to continue organizing assessments.");
    }

    @Override
    public String renderSchoolDebtClearedEmail(String schoolName) {
        return renderNotification(
            "The school has cleared its AI quota debt",
            "The school <strong>" + escapeHtml(schoolName) + "</strong> has enough quota again.",
            "Status",
            "Assessments can be organized as normal.");
    }

    // ---- subscription plan suspension/unsuspension -----------------------------

    @Override
    public String renderSchoolSubscriptionSuspendedEmail(String schoolName, String reason) {
        return renderNotification(
            "Subscription plan suspended",
            "The subscription plan of the school <strong>" + escapeHtml(schoolName) + "</strong> has just been suspended.",
            "Reason",
            escapeHtml(reason));
    }

    @Override
    public String renderSchoolSubscriptionUnsuspendedEmail(String schoolName) {
        return renderNotification(
            "Subscription plan unsuspended",
            "The subscription plan of the school <strong>" + escapeHtml(schoolName) + "</strong> has been unsuspended.",
            "Status",
            "The school can use the system as normal again.");
    }

    /**
     * The parameters here are already HTML (they contain {@code <strong>} tags) and HAVE been escaped in each
     * calling method — do not escape again here, otherwise the tags would show up as
     * text in the mail.
     */
    private String renderNotification(String heading, String intro, String panelTitle, String panelBody) {
        return loadTemplate(RESULT_NOTIFICATION_TEMPLATE)
            .replace("{{heading}}", heading)
            .replace("{{intro}}", intro)
            .replace("{{panelTitle}}", panelTitle)
            .replace("{{panelBody}}", panelBody);
    }

    private String loadTemplate(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load email template: " + path, e);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }


    
}
