package com.sep.vox.infrastructure.service;

import java.io.IOException;
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
    /** Khung dùng chung cho các thông báo vòng đời điểm — xem javadoc ở MailTemplatePort. */
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
    public String renderAppealRejectedEmail(String examName, String reason) {
        return loadTemplate(APPEAL_REJECTED_TEMPLATE)
            .replace("{{examName}}", escapeHtml(examName))
            .replace("{{reason}}", escapeHtml(reason));
    }

    @Override
    public String renderAppealPublishedEmail(String examName, String scoreBefore, String scoreAfter) {
        return loadTemplate(APPEAL_PUBLISHED_TEMPLATE)
            .replace("{{examName}}", escapeHtml(examName))
            .replace("{{scoreBefore}}", escapeHtml(scoreBefore))
            .replace("{{scoreAfter}}", escapeHtml(scoreAfter));
    }

    // ---- thông báo vòng đời điểm ------------------------------------------

    @Override
    public String renderResultReleasedEmail(String examName, String totalScore) {
        return renderNotification(
            "Điểm thi của bạn đã có",
            "Bài thi của bạn ở kỳ thi <strong>" + escapeHtml(examName) + "</strong> đã được chấm xong.",
            "Kết quả",
            "Tổng điểm: <strong>" + escapeHtml(totalScore) + "</strong>");
    }

    @Override
    public String renderResultRegradedEmail(
            String examName, String roundLabel, String scoreBefore, String scoreAfter) {
        return renderNotification(
            "Điểm thi của bạn đã thay đổi",
            "Bài thi của bạn ở kỳ thi <strong>" + escapeHtml(examName)
                + "</strong> vừa được " + escapeHtml(roundLabel) + ".",
            "Điểm sau khi chấm lại",
            "Điểm trước: " + escapeHtml(scoreBefore)
                + "<br><strong>Điểm sau: " + escapeHtml(scoreAfter) + "</strong>");
    }

    @Override
    public String renderResultInvalidatedEmail(String examName, String reason) {
        return renderNotification(
            "Bài thi của bạn đã bị vô hiệu",
            "Bài thi của bạn ở kỳ thi <strong>" + escapeHtml(examName)
                + "</strong> đã bị kết luận vi phạm quy chế.",
            "Lý do",
            escapeHtml(reason));
    }

    @Override
    public String renderResultInvalidClearedEmail(String examName, String reason) {
        return renderNotification(
            "Bài thi của bạn đã được khôi phục",
            "Sau khi xem xét lại, bài thi của bạn ở kỳ thi <strong>" + escapeHtml(examName)
                + "</strong> không bị coi là vi phạm và sẽ được chấm lại.",
            "Lý do",
            escapeHtml(reason));
    }

    @Override
    public String renderAppealApprovedEmail(String examName, String deadline) {
        return renderNotification(
            "Đơn phúc khảo của bạn đã được duyệt",
            "Đơn phúc khảo cho kỳ thi <strong>" + escapeHtml(examName)
                + "</strong> đã được duyệt và đang được chấm lại.",
            "Hạn xử lý",
            escapeHtml(deadline));
    }

    @Override
    public String renderResultOutcomeDecidedEmail(String examName, String outcomeLabel, String totalScore) {
        return renderNotification(
            "Kết quả cuối cùng của bạn",
            "Kỳ thi <strong>" + escapeHtml(examName) + "</strong> đã chốt kết quả.",
            "Kết quả",
            "Xếp loại: <strong>" + escapeHtml(outcomeLabel) + "</strong>"
                + "<br>Tổng điểm: " + escapeHtml(totalScore));
    }

    @Override
    public String renderGradingDeadlineReminderEmail(String examName, String roundLabel, String deadline) {
        return renderNotification(
            "Nhắc hạn chấm bài",
            "Bạn còn một bài chưa chấm xong ở kỳ thi <strong>" + escapeHtml(examName) + "</strong>.",
            "Chi tiết",
            "Vòng chấm: " + escapeHtml(roundLabel) + "<br><strong>Hạn: " + escapeHtml(deadline) + "</strong>");
    }

    @Override
    public String renderGradingDeclinedEmail(String examName, String teacherName, String reason) {
        return renderNotification(
            "Có giáo viên trả lại phân công chấm bài",
            "Một phân công ở kỳ thi <strong>" + escapeHtml(examName)
                + "</strong> vừa được trả lại và đang chờ giao cho người khác.",
            "Chi tiết",
            "Giáo viên: " + escapeHtml(teacherName) + "<br>Lý do: " + escapeHtml(reason));
    }

    @Override
    public String renderInvoicePaidEmail(
            String schoolName, String invoiceNumber, String itemTitle, String itemsHtml,
            String amountLabel, String paidAtLabel, String validUntilLabel) {
        return loadTemplate(INVOICE_PAID_TEMPLATE)
            .replace("{{schoolName}}", escapeHtml(schoolName))
            .replace("{{invoiceNumber}}", escapeHtml(invoiceNumber))
            .replace("{{itemTitle}}", escapeHtml(itemTitle))
            .replace("{{itemsHtml}}", itemsHtml)
            .replace("{{amountLabel}}", escapeHtml(amountLabel))
            .replace("{{paidAtLabel}}", escapeHtml(paidAtLabel))
            .replace("{{validUntilLabel}}", escapeHtml(validUntilLabel));
    }

    /**
     * Các tham số ở đây đã là HTML (có thẻ {@code <strong>}) và ĐÃ được escape ở từng
     * method gọi vào — đừng escape thêm lần nữa ở đây, nếu không thẻ sẽ hiện ra dưới
     * dạng chữ trong mail.
     */
    private String renderNotification(String heading, String intro, String panelTitle, String panelBody) {
        return loadTemplate(RESULT_NOTIFICATION_TEMPLATE)
            .replace("{{heading}}", heading)
            .replace("{{intro}}", intro)
            .replace("{{panelTitle}}", panelTitle)
            .replace("{{panelBody}}", panelBody);
    }

    private String loadTemplate(String path) {
        var resource = new ClassPathResource(path);
        try (var inputStream = resource.getInputStream()) {
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
