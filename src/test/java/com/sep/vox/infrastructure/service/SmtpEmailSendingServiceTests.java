package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Thiếu địa chỉ người gửi thì JavaMail ném "can't determine local email address" và KHÔNG mail nào
 * ra khỏi hệ thống. Lỗi đó từng sống rất lâu vì {@code @Async} nuốt mất exception -- test này khoá
 * lại cả hai điều: From luôn được set, và lỗi SMTP phải ném ra cho consumer thấy.
 */
class SmtpEmailSendingServiceTests {

    private static final String FROM = "noreply@vox.test";
    private static final String DISPLAY_NAME = "VOX";
    private static final String TO = "school-admin@example.com";

    private JavaMailSender javaMailSender;
    private SmtpEmailSendingService service;

    @BeforeEach
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        when(javaMailSender.createMimeMessage())
            .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        service = new SmtpEmailSendingService(javaMailSender, FROM, DISPLAY_NAME);
    }

    @Test
    void sendHtml_phai_set_dia_chi_nguoi_gui() throws Exception {
        service.sendHtml(TO, "Thiết lập mật khẩu", "<p>xin chào</p>");

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());

        var sent = captor.getValue();
        assertThat(sent.getFrom())
            .as("thiếu From là JavaMail ném can't determine local email address")
            .isNotNull()
            .hasSize(1);
        assertThat(sent.getFrom()[0].toString()).contains(FROM);
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo(TO);
    }

    @Test
    void sendHtml_gan_ten_hien_thi_vao_dia_chi_nguoi_gui() throws Exception {
        service.sendHtml(TO, "Thiết lập mật khẩu", "<p>xin chào</p>");

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());

        assertThat(captor.getValue().getFrom()[0].toString()).contains(DISPLAY_NAME);
    }

    /** Không có tên hiển thị vẫn phải gửi được -- tên chỉ là phần trang trí. */
    @Test
    void sendHtml_khong_co_ten_hien_thi_van_gui_duoc() throws Exception {
        var withoutDisplayName = new SmtpEmailSendingService(javaMailSender, FROM, "  ");

        withoutDisplayName.sendHtml(TO, "Thiết lập mật khẩu", "<p>xin chào</p>");

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()[0].toString()).isEqualTo(FROM);
    }

    @Test
    void send_phai_set_dia_chi_nguoi_gui() {
        service.send(TO, "Thông báo", "nội dung");

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo(FROM);
    }

    /**
     * Lỗi SMTP phải NÉM RA, không được nuốt. Consumer dựa vào đúng điều này để không ack và để
     * @RetryableTopic đưa event qua retry/DLT -- nuốt ở đây là mail mất vĩnh viễn.
     */
    @Test
    void loi_smtp_phai_nem_ra_cho_consumer_thay() {
        doThrow(new MailSendException("SMTP down")).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.sendHtml(TO, "Thiết lập mật khẩu", "<p>xin chào</p>"))
            .isInstanceOf(MailSendException.class);
    }
}
