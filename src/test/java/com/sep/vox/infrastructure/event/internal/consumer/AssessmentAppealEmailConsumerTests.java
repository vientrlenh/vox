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

import com.sep.vox.application.event.AssessmentAppealApprovedPayloadV1;
import com.sep.vox.application.event.AssessmentAppealPublishedPayloadV1;
import com.sep.vox.application.event.AssessmentAppealRejectedPayloadV1;
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

class AssessmentAppealEmailConsumerTests {

    private UserRepository userRepository;
    private MailSendingPort mailSendingPort;
    private MailTemplatePort mailTemplatePort;
    private ProcessedEventRepository processedEventRepository;
    private JsonMapper jsonMapper;
    private Acknowledgment ack;
    private AssessmentAppealEmailConsumer consumer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mailSendingPort = mock(MailSendingPort.class);
        mailTemplatePort = mock(MailTemplatePort.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        jsonMapper = JsonMapper.builder().build();
        ack = mock(Acknowledgment.class);
        consumer = new AssessmentAppealEmailConsumer(
            userRepository, mailSendingPort, mailTemplatePort, processedEventRepository, jsonMapper);
    }

    @Test
    void should_send_appeal_published_mail_to_student() throws Exception {
        UUID studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealPublishedEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        AssessmentAppealPublishedPayloadV1 payload = new AssessmentAppealPublishedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", new BigDecimal("5.5"), new BigDecimal("7.0"));

        consumer.consume(record(EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED, payload), ack);

        verify(mailTemplatePort).renderAppealPublishedEmail("Kỳ thi giữa kỳ", "5.5", "7.0");
        verify(mailSendingPort)
            .sendHtml("student@example.com", "Kết quả phúc khảo đã được công bố", "<html></html>");
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_send_appeal_rejected_mail_to_student() throws Exception {
        UUID studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealRejectedEmail(anyString(), anyString())).thenReturn("<html></html>");
        AssessmentAppealRejectedPayloadV1 payload = new AssessmentAppealRejectedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Nộp quá hạn");

        consumer.consume(record(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, payload), ack);

        verify(mailTemplatePort).renderAppealRejectedEmail("Kỳ thi giữa kỳ", "Nộp quá hạn");
        verify(mailSendingPort)
            .sendHtml("student@example.com", "Đơn phúc khảo không được chấp nhận", "<html></html>");
        verify(ack).acknowledge();
    }

    @Test
    void should_send_appeal_approved_mail_with_deadline_in_app_zone() {
        UUID studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealApprovedEmail(anyString(), anyString())).thenReturn("<html></html>");
        AssessmentAppealApprovedPayloadV1 payload = new AssessmentAppealApprovedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", Instant.parse("2026-08-01T03:00:00Z"));

        consumer.consume(record(EventTypeConstant.ASSESSMENT_APPEAL_APPROVED, payload), ack);

        // 03:00 UTC is 10:00 Vietnam time -- the format must follow DateMapper.DEFAULT_INPUT_ZONE,
        // not the container's UTC time.
        verify(mailTemplatePort).renderAppealApprovedEmail("Kỳ thi giữa kỳ", "10:00 01/08/2026");
        verify(ack).acknowledge();
    }

    @Test
    void should_use_placeholder_when_approved_deadline_is_null() {
        UUID studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealApprovedEmail(anyString(), anyString())).thenReturn("<html></html>");
        AssessmentAppealApprovedPayloadV1 payload = new AssessmentAppealApprovedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", null);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_APPEAL_APPROVED, payload), ack);

        verify(mailTemplatePort).renderAppealApprovedEmail("Kỳ thi giữa kỳ", "chưa đặt");
    }

    @Test
    void should_skip_when_event_already_processed() {
        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(UUID.class), anyString()))
            .thenReturn(true);
        AssessmentAppealRejectedPayloadV1 payload = new AssessmentAppealRejectedPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), "Kỳ thi giữa kỳ", "Nộp quá hạn");

        consumer.consume(record(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, payload), ack);

        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_when_student_not_found() {
        UUID studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.empty());
        AssessmentAppealRejectedPayloadV1 payload = new AssessmentAppealRejectedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Nộp quá hạn");

        // A student who has been deleted will never succeed however many times it is retried: mark
        // it done and skip it, do not push the message into the DLT.
        assertThatCode(() -> consumer.consume(record(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, payload), ack))
            .doesNotThrowAnyException();
        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_throw_when_event_type_is_unknown() {
        AssessmentAppealRejectedPayloadV1 payload = new AssessmentAppealRejectedPayloadV1(
            UUID.randomUUID(), UUID.randomUUID(), "Kỳ thi giữa kỳ", "Nộp quá hạn");

        assertThatThrownBy(() -> consumer.consume(record("KhongTonTai", payload), ack))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("KhongTonTai");
        verify(ack, never()).acknowledge();
    }

    @Test
    void should_throw_when_payload_is_invalid() {
        ConsumerRecord<String, String> record = rawRecord(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, "{khong-phai-json}");

        assertThatThrownBy(() -> consumer.consume(record, ack))
            .isInstanceOf(IllegalStateException.class);
        verify(ack, never()).acknowledge();
    }

    @Test
    void should_throw_when_mail_sending_fails() throws Exception {
        UUID studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(mailTemplatePort.renderAppealRejectedEmail(anyString(), anyString())).thenReturn("<html></html>");
        doThrow(new RejectedExecutionException("mail queue full"))
            .when(mailSendingPort).sendHtml(anyString(), anyString(), anyString());
        AssessmentAppealRejectedPayloadV1 payload = new AssessmentAppealRejectedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Nộp quá hạn");

        // Throwing it out is the only way for @RetryableTopic to know it should retry.
        assertThatThrownBy(() -> consumer.consume(record(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, payload), ack))
            .isInstanceOf(IllegalStateException.class);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack, never()).acknowledge();
    }

    private ConsumerRecord<String, String> record(String eventType, Object payload) {
        return rawRecord(eventType, jsonMapper.writeValueAsString(payload));
    }

    private ConsumerRecord<String, String> rawRecord(String eventType, String value) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("vox.assessment-appeal.v1", 0, 0L, "key", value);
        record.headers().add("eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private User student(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail(new Email("student@example.com"));
        user.setFullName(new FullName("Nguyễn Văn A"));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
