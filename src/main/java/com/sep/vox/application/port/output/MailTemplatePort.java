package com.sep.vox.application.port.output;

public interface MailTemplatePort {
    String renderPasswordSetUpEmail(String schoolAdminName, String schoolName, String passwordSetupUrl, String expiresIn);
    String renderSchoolUserPasswordSetUpEmail(String schoolUserName, String schoolName, String passwordSetupUrl, String expiresIn);
    String renderRejectRegisterFormEmail(String reason);
    String renderResetPasswordOtpEmail(String otp, String expiresIn);
    String renderRegisterVerificationOtpEmail(String otp, String expiresIn);
    String renderExamBlueprintReadyEmail(String blueprintName, String blueprintCode);
}
