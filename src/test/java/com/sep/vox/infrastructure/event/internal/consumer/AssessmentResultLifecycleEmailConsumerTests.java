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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.application.event.AssessmentResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.AssessmentResultInvalidatedPayloadV1;
import com.sep.vox.application.event.AssessmentResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.event.AssessmentResultRegradedPayloadV1;
import com.sep.vox.application.event.AssessmentResultReleasedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;

import tools.jackson.databind.json.JsonMapper;

class AssessmentResultLifecycleEmailConsumerTests {

    private UserRepository userRepository;
    private MailSendingPort mailSendingPort;
    private MailTemplatePort mailTemplatePort;
    private ProcessedEventRepository processedEventRepository;
    private JsonMapper jsonMapper;
    private Acknowledgment ack;
    private AssessmentResultLifecycleEmailConsumer consumer;

    private UUID studentId;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mailSendingPort = mock(MailSendingPort.class);
        mailTemplatePort = mock(MailTemplatePort.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        jsonMapper = JsonMapper.builder().build();
        ack = mock(Acknowledgment.class);
        consumer = new AssessmentResultLifecycleEmailConsumer(
            userRepository, mailSendingPort, mailTemplatePort, processedEventRepository, jsonMapper);

        studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
    }

    @Test
    void should_send_released_mail_to_student() throws Exception {
        when(mailTemplatePort.renderResultReleasedEmail(anyString(), anyString())).thenReturn("<html></html>");
        AssessmentResultReleasedPayloadV1 payload = new AssessmentResultReleasedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, payload), ack);

        verify(mailTemplatePort).renderResultReleasedEmail("Kỳ thi giữa kỳ", "8.5");
        verify(mailSendingPort).sendHtml("student@example.com", "Your assessment score is available", "<html></html>");
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_send_regraded_mail_with_round_label() throws Exception {
        when(mailTemplatePort.renderResultRegradedEmail(anyString(), anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        AssessmentResultRegradedPayloadV1 payload = new AssessmentResultRegradedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ",
            GradingRoundType.SPOT_CHECK.name(), new BigDecimal("8.5"), new BigDecimal("7.0"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_REGRADED, payload), ack);

        verify(mailTemplatePort)
            .renderResultRegradedEmail("Kỳ thi giữa kỳ", "spot-checked and regraded", "8.5", "7.0");
        verify(mailSendingPort).sendHtml("student@example.com", "Your assessment score has changed", "<html></html>");
    }

    @Test
    void should_use_fallback_label_when_round_type_is_unknown() throws Exception {
        when(mailTemplatePort.renderResultRegradedEmail(anyString(), anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        AssessmentResultRegradedPayloadV1 payload = new AssessmentResultRegradedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "VONG_LA", null, new BigDecimal("7.0"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        // An old message still in the topic may carry a round name that has been renamed/deleted.
        // Pushing the whole message into the DLT just because of a display label is the wrong
        // trade-off.
        assertThatCode(() -> consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_REGRADED, payload), ack))
            .doesNotThrowAnyException();
        verify(mailTemplatePort).renderResultRegradedEmail("Kỳ thi giữa kỳ", "regraded", "-", "7.0");
    }

    @Test
    void should_send_invalidated_mail_to_student() throws Exception {
        when(mailTemplatePort.renderResultInvalidatedEmail(anyString(), anyString())).thenReturn("<html></html>");
        AssessmentResultInvalidatedPayloadV1 payload = new AssessmentResultInvalidatedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Sử dụng tài liệu", UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED, payload), ack);

        verify(mailTemplatePort).renderResultInvalidatedEmail("Kỳ thi giữa kỳ", "Sử dụng tài liệu");
        verify(mailSendingPort)
            .sendHtml("student@example.com", "Your submission has been invalidated", "<html></html>");
    }

    @Test
    void should_send_invalid_cleared_mail_to_student() throws Exception {
        when(mailTemplatePort.renderResultInvalidClearedEmail(anyString(), anyString()))
            .thenReturn("<html></html>");
        AssessmentResultInvalidClearedPayloadV1 payload = new AssessmentResultInvalidClearedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "Xem lại video không có vi phạm", UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED, payload), ack);

        verify(mailSendingPort)
            .sendHtml("student@example.com", "Your submission has been reinstated", "<html></html>");
    }

    @Test
    void should_send_outcome_decided_mail_with_vietnamese_label() throws Exception {
        when(mailTemplatePort.renderResultOutcomeDecidedEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        AssessmentResultOutcomeDecidedPayloadV1 payload = new AssessmentResultOutcomeDecidedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "PASSED", new BigDecimal("8.5"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED, payload), ack);

        verify(mailTemplatePort).renderResultOutcomeDecidedEmail("Kỳ thi giữa kỳ", "Passed", "8.5");
        verify(mailSendingPort).sendHtml("student@example.com", "Your final result", "<html></html>");
    }

    @Test
    void should_label_outcome_as_not_passed_when_not_passed() throws Exception {
        when(mailTemplatePort.renderResultOutcomeDecidedEmail(anyString(), anyString(), anyString()))
            .thenReturn("<html></html>");
        AssessmentResultOutcomeDecidedPayloadV1 payload = new AssessmentResultOutcomeDecidedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", "FAILED", null, UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED, payload), ack);

        verify(mailTemplatePort).renderResultOutcomeDecidedEmail("Kỳ thi giữa kỳ", "Not passed", "-");
    }

    @Test
    void should_skip_when_event_already_processed() {
        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(UUID.class), anyString()))
            .thenReturn(true);
        AssessmentResultReleasedPayloadV1 payload = new AssessmentResultReleasedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, payload), ack);

        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_when_student_not_found() {
        UUID unknownStudentId = UUID.randomUUID();
        when(userRepository.findById(unknownStudentId)).thenReturn(Optional.empty());
        AssessmentResultReleasedPayloadV1 payload = new AssessmentResultReleasedPayloadV1(
            UUID.randomUUID(), unknownStudentId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        assertThatCode(() -> consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, payload), ack))
            .doesNotThrowAnyException();
        verifyNoInteractions(mailSendingPort);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(ack).acknowledge();
    }

    @Test
    void should_throw_when_event_type_is_unknown() {
        AssessmentResultReleasedPayloadV1 payload = new AssessmentResultReleasedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        assertThatThrownBy(() -> consumer.consume(record("KhongTonTai", payload), ack))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("KhongTonTai");
        verify(ack, never()).acknowledge();
    }

    @Test
    void should_throw_when_mail_sending_fails() throws Exception {
        when(mailTemplatePort.renderResultReleasedEmail(anyString(), anyString())).thenReturn("<html></html>");
        doThrow(new RejectedExecutionException("mail queue full"))
            .when(mailSendingPort).sendHtml(anyString(), anyString(), anyString());
        AssessmentResultReleasedPayloadV1 payload = new AssessmentResultReleasedPayloadV1(
            UUID.randomUUID(), studentId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"), UUID.randomUUID(), AssessmentKind.CENTRALIZED);

        assertThatThrownBy(() -> consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, payload), ack))
            .isInstanceOf(IllegalStateException.class);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(ack, never()).acknowledge();
    }

    private ConsumerRecord<String, String> record(String eventType, Object payload) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "vox.assessment-result-lifecycle.v1", 0, 0L, "key", jsonMapper.writeValueAsString(payload));
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
