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
