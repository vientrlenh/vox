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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import com.sep.vox.application.event.ExamAppealApprovedPayloadV1;
import com.sep.vox.application.event.ExamAppealPublishedPayloadV1;
import com.sep.vox.application.event.ExamAppealRejectedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;

import tools.jackson.databind.json.JsonMapper;

class ExamAppealEmailConsumerTests {

    private static final String CONSUMER_GROUP = "exam-appeal-email";

    private UserRepository userRepository;
    private MailSendingPort mailSendingPort;
    private MailTemplatePort mailTemplatePort;
    private ProcessedEventRepository processedEventRepository;
    private JsonMapper jsonMapper;
    private Acknowledgment ack;
    private ExamAppealEmailConsumer consumer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mailSendingPort = mock(MailSendingPort.class);
        mailTemplatePort = mock(MailTemplatePort.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        jsonMapper = JsonMapper.builder().build();
        ack = mock(Acknowledgment.class);
        consumer = new ExamAppealEmailConsumer(
            userRepository, mailSendingPort, mailTemplatePort, processedEventRepository, jsonMapper);
    }

    @Test
    void should_send_appeal_published_mail_to_student() throws Exception {
        var studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealPublishedEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        var payload = new ExamAppealPublishedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", new BigDecimal("5.5"), new BigDecimal("7.0"));

        consumer.consume(record(EventTypeConstant.EXAM_APPEAL_PUBLISHED, payload), ack);

        verify(mailTemplatePort).renderAppealPublishedEmail("Kỳ thi giữa kỳ", "5.5", "7.0");
        verify(mailSendingPort)
            .sendHtml("student@example.com", "Kết quả phúc khảo đã được công bố", "<html></html>");
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_send_appeal_rejected_mail_to_student() throws Exception {
        var studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealRejectedEmail(anyString(), anyString())).thenReturn("<html></html>");
        var payload = new ExamAppealRejectedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Nộp quá hạn");

        consumer.consume(record(EventTypeConstant.EXAM_APPEAL_REJECTED, payload), ack);

        verify(mailTemplatePort).renderAppealRejectedEmail("Kỳ thi giữa kỳ", "Nộp quá hạn");
        verify(mailSendingPort)
            .sendHtml("student@example.com", "Đơn phúc khảo không được chấp nhận", "<html></html>");
        verify(ack).acknowledge();
    }

    @Test
    void should_send_appeal_approved_mail_with_deadline_in_app_zone() {
        var studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealApprovedEmail(anyString(), anyString())).thenReturn("<html></html>");
        var payload = new ExamAppealApprovedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", Instant.parse("2026-08-01T03:00:00Z"));

        consumer.consume(record(EventTypeConstant.EXAM_APPEAL_APPROVED, payload), ack);

        // 03:00 UTC là 10:00 giờ Việt Nam — format phải theo DateMapper.DEFAULT_INPUT_ZONE,
        // không phải giờ UTC của container.
        verify(mailTemplatePort).renderAppealApprovedEmail("Kỳ thi giữa kỳ", "10:00 01/08/2026");
        verify(ack).acknowledge();
    }

    @Test
    void should_use_placeholder_when_approved_deadline_is_null() {
        var studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealApprovedEmail(anyString(), anyString())).thenReturn("<html></html>");
        var payload = new ExamAppealApprovedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", null);

        consumer.consume(record(EventTypeConstant.EXAM_APPEAL_APPROVED, payload), ack);

        verify(mailTemplatePort).renderAppealApprovedEmail("Kỳ thi giữa kỳ", "chưa đặt");
    }

    @Test
    void should_skip_when_event_already_processed() {
        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(UUID.class), anyString()))
            .thenReturn(true);
        var payload = new ExamAppealRejectedPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), "Kỳ thi giữa kỳ", "Nộp quá hạn");

        consumer.consume(record(EventTypeConstant.EXAM_APPEAL_REJECTED, payload), ack);

        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_when_student_not_found() {
        var studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.empty());
        var payload = new ExamAppealRejectedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Nộp quá hạn");

        // Học sinh đã bị xoá thì retry bao nhiêu lần cũng vô ích: đánh dấu xong và bỏ qua,
        // đừng đẩy message vào DLT.
        assertThatCode(() -> consumer.consume(record(EventTypeConstant.EXAM_APPEAL_REJECTED, payload), ack))
            .doesNotThrowAnyException();
        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_throw_when_event_type_is_unknown() {
        var payload = new ExamAppealRejectedPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), "Kỳ thi giữa kỳ", "Nộp quá hạn");

        assertThatThrownBy(() -> consumer.consume(record("KhongTonTai", payload), ack))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("KhongTonTai");
        verify(ack, never()).acknowledge();
    }

    @Test
    void should_throw_when_payload_is_invalid() {
        var record = rawRecord(EventTypeConstant.EXAM_APPEAL_REJECTED, "{khong-phai-json}");

        assertThatThrownBy(() -> consumer.consume(record, ack))
            .isInstanceOf(IllegalStateException.class);
        verify(ack, never()).acknowledge();
    }

    @Test
    void should_throw_when_mail_sending_fails() throws Exception {
        var studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealRejectedEmail(anyString(), anyString())).thenReturn("<html></html>");
        doThrow(new RejectedExecutionException("mail queue full"))
            .when(mailSendingPort).sendHtml(anyString(), anyString(), anyString());
        var payload = new ExamAppealRejectedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Nộp quá hạn");

        // Ném ra ngoài là cách duy nhất để @RetryableTopic biết mà thử lại.
        assertThatThrownBy(() -> consumer.consume(record(EventTypeConstant.EXAM_APPEAL_REJECTED, payload), ack))
            .isInstanceOf(IllegalStateException.class);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack, never()).acknowledge();
    }

    private ConsumerRecord<String, String> record(String eventType, Object payload) {
        return rawRecord(eventType, jsonMapper.writeValueAsString(payload));
    }

    private ConsumerRecord<String, String> rawRecord(String eventType, String value) {
        var record = new ConsumerRecord<>("vox.exam-appeal.v1", 0, 0L, "key", value);
        record.headers().add("eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private User student(UUID id) {
        var user = new User();
        user.setId(id);
        user.setEmail(new Email("student@example.com"));
        user.setFullName(new FullName("Nguyễn Văn A"));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
