package com.sep.vox.application.port.output;

public interface MailTemplatePort {
    String renderPasswordSetUpEmail(String schoolAdminName, String schoolName, String passwordSetupUrl, String expiresIn);
    String renderSchoolUserPasswordSetUpEmail(String schoolUserName, String schoolName, String passwordSetupUrl, String expiresIn);
    String renderRejectRegisterFormEmail(String reason);
    String renderResetPasswordOtpEmail(String otp, String expiresIn);
    String renderRegisterVerificationOtpEmail(String otp, String expiresIn);
    String renderExamBlueprintReadyEmail(String blueprintName, String blueprintCode);
    String renderAppealRejectedEmail(String examName, String reason);
    String renderAppealPublishedEmail(String examName, String scoreBefore, String scoreAfter);

    // ---- thông báo vòng đời điểm ------------------------------------------
    //
    // Bảy mail dưới đây dùng chung MỘT template (`result-notification.html`) với các
    // ô tiêu đề / dẫn nhập / nội dung. Chúng chỉ khác nhau ở CHỮ, nên bảy file HTML
    // gần giống hệt sẽ là bảy chỗ phải sửa mỗi lần đổi thương hiệu hoặc bố cục.
    // Chữ nghĩa vẫn nằm ở tầng render, không rò xuống listener.

    /** Điểm được công bố lần đầu. */
    String renderResultReleasedEmail(String examName, String totalScore);

    /** Điểm ĐÃ công bố nay thay đổi (hậu kiểm hoặc phúc khảo). */
    String renderResultRegradedEmail(String examName, String roundLabel, String scoreBefore, String scoreAfter);

    String renderResultInvalidatedEmail(String examName, String reason);

    String renderResultInvalidClearedEmail(String examName, String reason);

    /** Đơn phúc khảo đã được duyệt và đã có người chấm. */
    String renderAppealApprovedEmail(String examName, String deadline);

    /** Kết quả chốt PASSED/FAILED sau khi kỳ thi công bố. */
    String renderResultOutcomeDecidedEmail(String examName, String outcomeLabel, String totalScore);

    /** Nhắc giáo viên phân công sắp/đã tới hạn. */
    String renderGradingDeadlineReminderEmail(String examName, String roundLabel, String deadline);

    /** Báo admin có giáo viên trả lại phân công. */
    String renderGradingDeclinedEmail(String examName, String teacherName, String reason);
}
