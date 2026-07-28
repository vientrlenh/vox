package com.sep.vox.infrastructure.event.internal.listener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.SchoolUserPasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;

@Component
public class SchoolUserPasswordSetUpEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolUserPasswordSetUpEmailListener.class);

    private static final String SUBJECT = "Thiết lập mật khẩu tài khoản VOX";
    private static final String EXPIRES_IN = "48 giờ";

    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final String passwordSetupBaseUrl;

    public SchoolUserPasswordSetUpEmailListener(
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort,
            @Value("${app.frontend.password-setup-url}") String passwordSetupBaseUrl) {
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
        this.passwordSetupBaseUrl = passwordSetupBaseUrl;
    }

    /**
     * Listener chạy AFTER_COMMIT ngay trên thread đang import: user đã được tạo và commit
     * xong rồi, nên lỗi gửi mail chỉ được log. Ném ra ngoài thì exception đi ngược lên
     * {@code SchoolUserImportCommitHandler} và {@code ImportCommitService} đánh cả phiên
     * import thành FAILED — mất trạng thái của mọi dòng đã xử lý vì một cái mail.
     *
     * <p>User vẫn tự đặt được mật khẩu qua luồng "quên mật khẩu" nếu mail này thất bại.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SchoolUserPasswordSetUpEmailRequestedEvent event) {
        try {
            var setupUrl = buildPasswordSetupUrl(event.userId(), event.rawToken());
            var html = mailTemplatePort.renderSchoolUserPasswordSetUpEmail(
                event.schoolUserName(),
                event.schoolName(),
                setupUrl,
                EXPIRES_IN
            );

            mailSendingPort.sendHtml(event.to(), SUBJECT, html);
        } catch (Exception e) {
            LOGGER.error("Không gửi được mail thiết lập mật khẩu cho {}: {}", event.to(), e.getMessage());
        }
    }

    private String buildPasswordSetupUrl(UUID userId, String rawToken) {
        var separator = passwordSetupBaseUrl.contains("?") ? "&" : "?";
        return passwordSetupBaseUrl + separator + "userId=" + encode(userId.toString()) + "&token=" + encode(rawToken);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}