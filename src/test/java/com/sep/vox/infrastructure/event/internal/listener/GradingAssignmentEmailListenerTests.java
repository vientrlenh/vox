package com.sep.vox.infrastructure.event.internal.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.event.GradingDeadlineReminderEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;

class GradingAssignmentEmailListenerTests {

    private UserRepository userRepository;
    private MailSendingPort mailSendingPort;
    private MailTemplatePort mailTemplatePort;
    private GradingAssignmentEmailListener listener;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mailSendingPort = mock(MailSendingPort.class);
        mailTemplatePort = mock(MailTemplatePort.class);
        listener = new GradingAssignmentEmailListener(userRepository, mailSendingPort, mailTemplatePort);
    }

    @Test
    void should_send_deadline_reminder_to_teacher() throws Exception {
        var teacherId = UUID.randomUUID();
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher(teacherId)));
        when(mailTemplatePort.renderGradingDeadlineReminderEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");

        listener.onDeadlineReminder(reminderEvent(teacherId));

        verify(mailSendingPort).sendHtml("teacher@example.com", "Nhắc hạn chấm bài", "<html></html>");
    }

    @Test
    void should_not_propagate_when_reminder_mail_fails() throws Exception {
        var teacherId = UUID.randomUUID();
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher(teacherId)));
        when(mailTemplatePort.renderGradingDeadlineReminderEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        doThrow(new RejectedExecutionException("mail queue full"))
            .when(mailSendingPort).sendHtml(anyString(), anyString(), anyString());

        // GradingDeadlineReminderBatch commit reminded_at TRƯỚC khi gửi mail, nên một mail
        // lỗi không được làm các phân công còn lại trong lô mất nhắc hạn.
        assertThatCode(() -> listener.onDeadlineReminder(reminderEvent(teacherId)))
            .doesNotThrowAnyException();
    }

    private GradingDeadlineReminderEvent reminderEvent(UUID teacherId) {
        return new GradingDeadlineReminderEvent(
            UUID.randomUUID(),
            teacherId,
            "Kỳ thi giữa kỳ",
            GradingRoundType.INITIAL.name(),
            OffsetDateTime.parse("2026-08-01T10:00:00Z").toInstant()
        );
    }

    private User teacher(UUID id) {
        var user = new User();
        user.setId(id);
        user.setEmail(new Email("teacher@example.com"));
        user.setFullName(new FullName("Nguyễn Văn B"));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
