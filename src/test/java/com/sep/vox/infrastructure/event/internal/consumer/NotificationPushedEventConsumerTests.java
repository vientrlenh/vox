package com.sep.vox.infrastructure.event.internal.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import com.sep.vox.application.response.output.PushDispatchResult;
import com.sep.vox.application.response.output.PushMessage;
import com.sep.vox.application.event.ExamAppealApprovedPayloadV1;
import com.sep.vox.application.event.ExamAppealPublishedPayloadV1;
import com.sep.vox.application.event.ExamAppealRejectedPayloadV1;
import com.sep.vox.application.event.ExamResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.ExamResultInvalidatedPayloadV1;
import com.sep.vox.application.event.ExamResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.event.ExamResultRegradedPayloadV1;
import com.sep.vox.application.event.ExamResultReleasedPayloadV1;
import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.domain.model.notification.NotificationPreference;
import com.sep.vox.domain.repository.NotificationDeviceRepository;
import com.sep.vox.domain.repository.NotificationPreferenceRepository;
import com.sep.vox.domain.repository.NotificationRepository;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

class NotificationPushedEventConsumerTests {

    private static final String FID = "fid-aaaaaaaaaaaaaaaaaaaa";

    private NotificationRepository notificationRepository;
    private NotificationDeviceRepository notificationDeviceRepository;
    private NotificationPreferenceRepository notificationPreferenceRepository;
    private ProcessedEventRepository processedEventRepository;
    private PushNotificationPort pushNotificationPort;
    private JsonMapper jsonMapper;
    private Acknowledgment ack;
    private NotificationPushedEventConsumer consumer;

    private UUID userId;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationDeviceRepository = mock(NotificationDeviceRepository.class);
        notificationPreferenceRepository = mock(NotificationPreferenceRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        pushNotificationPort = mock(PushNotificationPort.class);
        jsonMapper = JsonMapper.builder().build();
        ack = mock(Acknowledgment.class);

        consumer = new NotificationPushedEventConsumer(
            notificationRepository,
            notificationDeviceRepository,
            notificationPreferenceRepository,
            processedEventRepository,
            pushNotificationPort,
            jsonMapper);

        userId = UUID.randomUUID();

        when(notificationRepository.saveIfAbsent(any()))
            .thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        when(notificationPreferenceRepository.findByUserIdAndCategory(any(), any()))
            .thenReturn(Optional.empty());
        when(notificationDeviceRepository.findByUserId(any())).thenReturn(List.of(device(FID)));
        when(pushNotificationPort.isEnabled()).thenReturn(true);
        when(pushNotificationPort.send(any(), anyList()))
            .thenReturn(new PushDispatchResult(1, List.of(), List.of()));
    }

    /**
     * Ánh xạ category và nhánh switch dựng nội dung là hai danh sách riêng biệt, rất dễ
     * thêm một bên mà quên bên kia. Test này chạy qua đủ 10 eventType đã khai báo và bắt
     * lỗi ngay nếu switch rơi vào default.
     */
    @Test
    void should_create_notification_for_every_mapped_event_type() {
        var count = allEventTypes().size();

        for (int i = 0; i < count; i++) {
            // setUp() sinh userId mới, nên payload phải được dựng lại SAU nó -- dựng trước
            // thì payload giữ userId cũ và phép so sánh người nhận trở nên vô nghĩa.
            setUp();
            var testCase = allEventTypes().get(i);

            consumer.consume(record(testCase.eventType(), testCase.payload()), ack);

            var captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).saveIfAbsent(captor.capture());
            var saved = captor.getValue();

            assertThat(saved.getUserId())
                .as("người nhận của %s", testCase.eventType())
                .isEqualTo(userId);
            assertThat(saved.getTitle())
                .as("tiêu đề của %s", testCase.eventType())
                .isNotBlank();
            assertThat(saved.getEventType()).isEqualTo(testCase.eventType());
            verify(ack).acknowledge();
        }
    }

    @Test
    void should_cover_exactly_the_mapped_event_types() {
        var covered = allEventTypes().stream().map(c -> c.eventType()).toList();
        assertThat(covered).containsExactlyInAnyOrderElementsOf(NotificationCategory.mappedEventTypes());
    }

    @Test
    void should_not_map_events_for_users_without_devices() {
        assertThat(NotificationCategory.isMapped(EventTypeConstant.USER_CREATED)).isFalse();
        assertThat(NotificationCategory.isMapped(EventTypeConstant.REGISTER_FORM_REJECTED)).isFalse();
    }

    /** Dòng DB là bản ghi bền vững, phải được ghi trước khi chạm tới FCM. */
    @Test
    void should_persist_notification_before_pushing() {
        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        var order = inOrder(notificationRepository, pushNotificationPort);
        order.verify(notificationRepository).saveIfAbsent(any());
        order.verify(pushNotificationPort).send(any(), anyList());
    }

    @Test
    void should_still_persist_when_user_has_no_device() {
        when(notificationDeviceRepository.findByUserId(any())).thenReturn(List.of());

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        verify(notificationRepository).saveIfAbsent(any());
        verify(pushNotificationPort, never()).send(any(), anyList());
        verify(processedEventRepository).save(any());
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_push_when_preference_disabled() {
        when(notificationPreferenceRepository.findByUserIdAndCategory(any(), any()))
            .thenReturn(Optional.of(new NotificationPreference(
                userId, NotificationCategory.EXAM_RESULT, false, true, Instant.now())));

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        verify(notificationRepository).saveIfAbsent(any());
        verify(pushNotificationPort, never()).send(any(), anyList());
    }

    @Test
    void should_push_by_default_when_preference_row_is_absent() {
        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        verify(pushNotificationPort).send(any(), anyList());
    }

    /** Chỉ nhóm stale mới bị xoá; lỗi tạm thời phải giữ nguyên thiết bị. */
    @Test
    void should_delete_only_stale_installation_ids() {
        when(pushNotificationPort.send(any(), anyList()))
            .thenReturn(new PushDispatchResult(0, List.of("fid-dead"), List.of("fid-flaky")));

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(notificationDeviceRepository).deleteByInstallationIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactly("fid-dead");
    }

    @Test
    void should_not_push_twice_when_notification_already_exists() {
        // doReturn thay vì when(...): mock đã được stub bằng thenAnswer ở setUp, nên
        // when(mock.saveIfAbsent(any())) sẽ thực sự gọi answer cũ với đối số null.
        doReturn(Optional.empty()).when(notificationRepository).saveIfAbsent(any());

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        verify(pushNotificationPort, never()).send(any(), anyList());
        verify(processedEventRepository).save(any());
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_already_processed_event() {
        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(), any())).thenReturn(true);

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationPort);
        verify(ack).acknowledge();
    }

    @Test
    void should_carry_event_type_into_push_data_for_deep_linking() {
        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        var captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("eventType", EventTypeConstant.EXAM_RESULT_RELEASED)
            .containsKey("candidateResultId");
    }

    // --- helpers ---------------------------------------------------------------

    private record TestCase(String eventType, Object payload) {}

    private List<TestCase> allEventTypes() {
        var examName = "Kỳ thi giữa kỳ";
        var id = UUID.randomUUID();
        return List.of(
            new TestCase(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()),
            new TestCase(EventTypeConstant.EXAM_RESULT_REGRADED, new ExamResultRegradedPayloadV1(
                id, userId, examName, "FIRST", new BigDecimal("7.0"), new BigDecimal("8.0"))),
            new TestCase(EventTypeConstant.EXAM_RESULT_INVALIDATED, new ExamResultInvalidatedPayloadV1(
                id, userId, examName, "Gian lận")),
            new TestCase(EventTypeConstant.EXAM_RESULT_INVALID_CLEARED, new ExamResultInvalidClearedPayloadV1(
                id, userId, examName, "Đã xác minh lại")),
            new TestCase(EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED, new ExamResultOutcomeDecidedPayloadV1(
                id, userId, examName, "PASSED", new BigDecimal("8.0"))),

            new TestCase(EventTypeConstant.EXAM_APPEAL_PUBLISHED, new ExamAppealPublishedPayloadV1(
                id, userId, examName, new BigDecimal("6.0"), new BigDecimal("7.5"))),
            new TestCase(EventTypeConstant.EXAM_APPEAL_REJECTED, new ExamAppealRejectedPayloadV1(
                id, userId, examName, "Không đủ căn cứ")),
            new TestCase(EventTypeConstant.EXAM_APPEAL_APPROVED, new ExamAppealApprovedPayloadV1(
                id, userId, examName, Instant.parse("2026-09-01T03:00:00Z"))),

            new TestCase(EventTypeConstant.GRADING_DEADLINE_REMINDER, new GradingDeadlineReminderPayloadV1(
                id, userId, examName, "FIRST", Instant.parse("2026-09-01T03:00:00Z"))),
            // Người nhận là assignedBy (admin đã giao việc), không phải teacherId.
            new TestCase(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, new GradingAssignmentDeclinedPayloadV1(
                id, id, UUID.randomUUID(), userId, examName, "Bận lịch coi thi"))
        );
    }

    private ExamResultReleasedPayloadV1 releasedPayload() {
        return new ExamResultReleasedPayloadV1(UUID.randomUUID(), userId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"));
    }

    private NotificationDevice device(String installationId) {
        return new NotificationDevice(
            userId, "device-1", NotificationDevicePlatform.WEB, installationId, Instant.now(), Instant.now());
    }

    private ConsumerRecord<String, String> record(String eventType, Object payload) {
        var value = jsonMapper.writeValueAsString(payload);
        var record = new ConsumerRecord<>("vox.exam-result-lifecycle.v1", 0, 0L, "key", value);
        record.headers().add("eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
