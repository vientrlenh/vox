package com.sep.vox.infrastructure.event.internal.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;

import tools.jackson.databind.json.JsonMapper;

class GradingAssignmentEmailConsumerTests {

    private UserRepository userRepository;
    private MailSendingPort mailSendingPort;
    private MailTemplatePort mailTemplatePort;
    private ProcessedEventRepository processedEventRepository;
    private JsonMapper jsonMapper;
    private Acknowledgment ack;
    private GradingAssignmentEmailConsumer consumer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mailSendingPort = mock(MailSendingPort.class);
        mailTemplatePort = mock(MailTemplatePort.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        jsonMapper = JsonMapper.builder().build();
        ack = mock(Acknowledgment.class);
        consumer = new GradingAssignmentEmailConsumer(
            userRepository, mailSendingPort, mailTemplatePort, processedEventRepository, jsonMapper);
    }

    @Test
    void should_send_deadline_reminder_to_teacher() throws Exception {
        var teacherId = UUID.randomUUID();
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(user(teacherId, "teacher@example.com")));
        when(mailTemplatePort.renderGradingDeadlineReminderEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        var payload = new GradingDeadlineReminderPayloadV1(
            UUID.randomUUID(), teacherId, "Kỳ thi giữa kỳ",
            GradingRoundType.INITIAL.name(), Instant.parse("2026-08-01T03:00:00Z"));

        consumer.consume(record(EventTypeConstant.GRADING_DEADLINE_REMINDER, payload), ack);

        // 03:00 UTC là 10:00 giờ Việt Nam.
        verify(mailTemplatePort)
            .renderGradingDeadlineReminderEmail("Kỳ thi giữa kỳ", "Chấm lần đầu", "10:00 01/08/2026");
        verify(mailSendingPort).sendHtml("teacher@example.com", "Nhắc hạn chấm bài", "<html></html>");
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_retry_when_reminder_mail_fails() throws Exception {
        var teacherId = UUID.randomUUID();
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(user(teacherId, "teacher@example.com")));
        when(mailTemplatePort.renderGradingDeadlineReminderEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        doThrow(new RejectedExecutionException("mail queue full"))
            .when(mailSendingPort).sendHtml(anyString(), anyString(), anyString());
        var payload = new GradingDeadlineReminderPayloadV1(
            UUID.randomUUID(), teacherId, "Kỳ thi giữa kỳ",
            GradingRoundType.INITIAL.name(), Instant.parse("2026-08-01T03:00:00Z"));

        // Khác hẳn listener cũ: ở đó lỗi phải nuốt vì reminded_at đã commit và không có gì
        // gửi lại. Giờ outbox giữ message nên ném ra để @RetryableTopic thử lại mới đúng.
        assertThatThrownBy(() -> consumer.consume(record(EventTypeConstant.GRADING_DEADLINE_REMINDER, payload), ack))
            .isInstanceOf(IllegalStateException.class);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack, never()).acknowledge();
    }

    @Test
    void should_use_placeholders_when_reminder_fields_are_null() throws Exception {
        var teacherId = UUID.randomUUID();
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(user(teacherId, "teacher@example.com")));
        when(mailTemplatePort.renderGradingDeadlineReminderEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        var payload = new GradingDeadlineReminderPayloadV1(
            UUID.randomUUID(), teacherId, null, null, null);

        consumer.consume(record(EventTypeConstant.GRADING_DEADLINE_REMINDER, payload), ack);

        verify(mailTemplatePort).renderGradingDeadlineReminderEmail("-", "-", "-");
    }

    @Test
    void should_send_declined_mail_to_assigner_not_to_teacher() throws Exception {
        var assignedBy = UUID.randomUUID();
        var teacherId = UUID.randomUUID();
        when(userRepository.findById(assignedBy)).thenReturn(Optional.of(user(assignedBy, "admin@example.com")));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(user(teacherId, "teacher@example.com")));
        when(mailTemplatePort.renderGradingDeclinedEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        var payload = new GradingAssignmentDeclinedPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), teacherId, assignedBy, "Kỳ thi giữa kỳ", "Bận công tác");

        consumer.consume(record(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, payload), ack);

        verify(mailTemplatePort).renderGradingDeclinedEmail("Kỳ thi giữa kỳ", "Nguyễn Văn B", "Bận công tác");
        verify(mailSendingPort).sendHtml(
            "admin@example.com", "Có giáo viên trả lại phân công chấm bài", "<html></html>");
        verify(ack).acknowledge();
    }

    @Test
    void should_use_placeholder_teacher_name_when_teacher_not_found() throws Exception {
        var assignedBy = UUID.randomUUID();
        var teacherId = UUID.randomUUID();
        when(userRepository.findById(assignedBy)).thenReturn(Optional.of(user(assignedBy, "admin@example.com")));
        when(userRepository.findById(teacherId)).thenReturn(Optional.empty());
        when(mailTemplatePort.renderGradingDeclinedEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        var payload = new GradingAssignmentDeclinedPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), teacherId, assignedBy, null, null);

        consumer.consume(record(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, payload), ack);

        verify(mailTemplatePort).renderGradingDeclinedEmail("-", "-", "-");
    }

    @Test
    void should_skip_when_event_already_processed() {
        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(UUID.class), anyString()))
            .thenReturn(true);
        var payload = new GradingDeadlineReminderPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), "Kỳ thi giữa kỳ", null, null);

        consumer.consume(record(EventTypeConstant.GRADING_DEADLINE_REMINDER, payload), ack);

        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_when_assigner_not_found() {
        var assignedBy = UUID.randomUUID();
        when(userRepository.findById(assignedBy)).thenReturn(Optional.empty());
        var payload = new GradingAssignmentDeclinedPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), assignedBy, "Kỳ thi giữa kỳ", "Bận");

        assertThatCode(() -> consumer.consume(record(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, payload), ack))
            .doesNotThrowAnyException();
        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_throw_when_event_type_is_unknown() {
        var payload = new GradingDeadlineReminderPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), "Kỳ thi giữa kỳ", null, null);

        assertThatThrownBy(() -> consumer.consume(record("KhongTonTai", payload), ack))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("KhongTonTai");
        verify(ack, never()).acknowledge();
    }

    private ConsumerRecord<String, String> record(String eventType, Object payload) {
        var record = new ConsumerRecord<>(
            "vox.grading-assignment.v1", 0, 0L, "key", jsonMapper.writeValueAsString(payload));
        record.headers().add("eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private User user(UUID id, String email) {
        var user = new User();
        user.setId(id);
        user.setEmail(new Email(email));
        user.setFullName(new FullName("Nguyễn Văn B"));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
