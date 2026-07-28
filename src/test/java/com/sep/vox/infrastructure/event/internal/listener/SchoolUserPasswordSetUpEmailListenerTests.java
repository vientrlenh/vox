package com.sep.vox.infrastructure.event.internal.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.SchoolUserPasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;

class SchoolUserPasswordSetUpEmailListenerTests {

    private MailSendingPort mailSendingPort;
    private MailTemplatePort mailTemplatePort;
    private SchoolUserPasswordSetUpEmailListener listener;

    @BeforeEach
    void setUp() {
        mailSendingPort = mock(MailSendingPort.class);
        mailTemplatePort = mock(MailTemplatePort.class);
        listener = new SchoolUserPasswordSetUpEmailListener(
            mailSendingPort, mailTemplatePort, "https://vox.test/password-setup");
    }

    @Test
    void should_send_password_setup_email_with_setup_url() throws Exception {
        var userId = UUID.randomUUID();
        when(mailTemplatePort.renderSchoolUserPasswordSetUpEmail(anyString(), anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");

        listener.handle(event(userId, "student@example.com"));

        var urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailTemplatePort).renderSchoolUserPasswordSetUpEmail(
            eq("Nguyễn Văn A"), eq("Trường Test"), urlCaptor.capture(), anyString());
        assertThat(urlCaptor.getValue())
            .isEqualTo("https://vox.test/password-setup?userId=" + userId + "&token=rawToken");
        verify(mailSendingPort).sendHtml("student@example.com", "Thiết lập mật khẩu tài khoản VOX", "<html></html>");
    }

    @Test
    void should_not_propagate_when_mail_dispatch_is_rejected() throws Exception {
        when(mailTemplatePort.renderSchoolUserPasswordSetUpEmail(anyString(), anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        doThrow(new RejectedExecutionException("mail queue full"))
            .when(mailSendingPort).sendHtml(anyString(), anyString(), anyString());

        // Mail chỉ là best-effort: lỗi gửi mail không được làm hỏng phiên import user.
        assertThatCode(() -> listener.handle(event(UUID.randomUUID(), "student@example.com")))
            .doesNotThrowAnyException();
    }

    @Test
    void should_not_propagate_when_template_rendering_fails() {
        when(mailTemplatePort.renderSchoolUserPasswordSetUpEmail(any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("template lỗi"));

        assertThatCode(() -> listener.handle(event(UUID.randomUUID(), "student@example.com")))
            .doesNotThrowAnyException();
    }

    private SchoolUserPasswordSetUpEmailRequestedEvent event(UUID userId, String to) {
        return new SchoolUserPasswordSetUpEmailRequestedEvent(to, "Nguyễn Văn A", "Trường Test", userId, "rawToken");
    }
}
