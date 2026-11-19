package com.sep.vox.infrastructure.event.internal.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.kafka.support.Acknowledgment;

import com.sep.vox.application.response.output.PushDispatchResult;
import com.sep.vox.application.response.output.PushMessage;
import com.sep.vox.application.event.ExamAppealApprovedPayloadV1;
import com.sep.vox.application.event.ExamAppealPublishedPayloadV1;
import com.sep.vox.application.event.ExamAppealRejectedPayloadV1;
import com.sep.vox.application.event.ExamBlueprintVersionPublishedEvent;
import com.sep.vox.application.event.ExamHumanGradingRequiredPayloadV1;
import com.sep.vox.application.event.ExamResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.ExamResultInvalidatedPayloadV1;
import com.sep.vox.application.event.ExamResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.event.ExamResultRegradedPayloadV1;
import com.sep.vox.application.event.ExamResultReleasedPayloadV1;
import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.event.GradingAssignmentOpenedPayloadV1;
import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.event.InvoicePaidPayloadV1;
import com.sep.vox.application.event.SchoolDebtCapExceededPayloadV1;
import com.sep.vox.application.event.SchoolDebtClearedPayloadV1;
import com.sep.vox.application.event.SchoolLockedDueToDebtPayloadV1;
import com.sep.vox.application.event.SchoolQuotaUsageWarningPayloadV1;
import com.sep.vox.application.event.SchoolSubscriptionSuspendedPayloadV1;
import com.sep.vox.application.event.SchoolSubscriptionUnsuspendedPayloadV1;
import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.invoice.InvoiceSourceType;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.domain.model.notification.NotificationPreference;
import com.sep.vox.domain.model.notification.NotificationTarget;
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

    /**
     * Chạy task ngay trên luồng gọi. Đây chính là lý do consumer nhận {@link Executor} thay
     * vì {@code ThreadPoolTaskExecutor} hay {@code @Async}: mọi assertion về push bên dưới
     * giữ nguyên tính tất định, không cần await và không thể flaky.
     */
    private Executor executor;

    private UUID userId;

    /** Hai khoá điều hướng mà màn hình đích cần ngoài id của chính thực thể. */
    private UUID sessionId;
    private UUID examId;
    private UUID blueprintId;
    private UUID versionId;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationDeviceRepository = mock(NotificationDeviceRepository.class);
        notificationPreferenceRepository = mock(NotificationPreferenceRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        pushNotificationPort = mock(PushNotificationPort.class);
        jsonMapper = JsonMapper.builder().build();
        ack = mock(Acknowledgment.class);
        executor = r -> r.run();

        consumer = new NotificationPushedEventConsumer(
            notificationRepository,
            notificationDeviceRepository,
            notificationPreferenceRepository,
            processedEventRepository,
            pushNotificationPort,
            jsonMapper,
            executor);

        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        examId = UUID.randomUUID();
        blueprintId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        when(notificationRepository.saveIfAbsent(any()))
            .thenAnswer(invocation -> Optional.of(withId(invocation.getArgument(0))));
        when(notificationPreferenceRepository.findByUserIdAndCategory(any(), any()))
            .thenReturn(Optional.empty());
        when(notificationDeviceRepository.findByUserId(any())).thenReturn(List.of(device(FID)));
        when(pushNotificationPort.isEnabled()).thenReturn(true);
        when(pushNotificationPort.send(any(), anyList()))
            .thenReturn(new PushDispatchResult(1, List.of(), List.of()));
    }

    /**
     * The category mapping and the switch branch that builds the content are two separate lists, so
     * it is very easy to add one and forget the other. This test runs through all 10 declared
     * eventTypes and catches it right away if the switch falls into default.
     */
    @Test
    void should_create_notification_for_every_mapped_event_type() {
        int count = allEventTypes().size();

        for (int i = 0; i < count; i++) {
            // setUp() generates a new userId, so the payload must be rebuilt AFTER it -- if built
            // before, the payload keeps the old userId and the recipient comparison becomes
            // meaningless.
            setUp();
            NotificationPushedEventConsumerTests.TestCase testCase = allEventTypes().get(i);

            consumer.consume(record(testCase.eventType(), testCase.payload()), ack);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).saveIfAbsent(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.getUserId())
                .as("người nhận của %s", testCase.eventType())
                .isEqualTo(userId);
            assertThat(saved.getTitle())
                .as("tiêu đề của %s", testCase.eventType())
                .isNotBlank();
            assertThat(saved.getEventType()).isEqualTo(testCase.eventType());
            // The payload is the only thing the client has to know which screen to open, so every
            // eventType must write its target into it -- no notification should be one that "goes
            // nowhere when tapped".
            assertThat(saved.getPayload())
                .as("target trong payload của %s", testCase.eventType())
                .contains(NotificationTarget.of(testCase.eventType()).name());
            verify(ack).acknowledge();
        }
    }

    @Test
    void should_cover_exactly_the_mapped_event_types() {
        List<String> covered = allEventTypes().stream().map(c -> c.eventType()).toList();
        assertThat(covered).containsExactlyInAnyOrderElementsOf(NotificationCategory.mappedEventTypes());
    }

    /** One event, one separate notification row for each school admin in the payload. */
    @Test
    void should_create_one_notification_per_recipient_when_event_fans_out() {
        UUID admin1 = UUID.randomUUID();
        UUID admin2 = UUID.randomUUID();
        UUID admin3 = UUID.randomUUID();

        consumer.consume(record(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED,
            new ExamBlueprintVersionPublishedEvent(
                List.of(admin1, admin2, admin3), "BP-01", "Blueprint Toán 12", blueprintId, versionId)), ack);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).saveIfAbsent(captor.capture());
        assertThat(captor.getAllValues()).extracting(n -> n.getUserId())
            .containsExactlyInAnyOrder(admin1, admin2, admin3);
        verify(ack).acknowledge();
    }

    /**
     * A duplicate id in the payload costs only one write that uk_notifications_user_event blocks.
     * The payload has traveled through Kafka, so the consumer cannot control it and must filter it
     * itself.
     */
    @Test
    void should_ignore_duplicate_and_null_recipients_in_fan_out_payload() {
        UUID admin = UUID.randomUUID();

        consumer.consume(record(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED,
            new ExamBlueprintVersionPublishedEvent(
                Arrays.asList(admin, admin, null), "BP-01", "Blueprint Toán 12", blueprintId, versionId)), ack);

        verify(notificationRepository, times(1)).saveIfAbsent(any());
        verify(ack).acknowledge();
    }

    /**
     * A crash midway in the previous run may have already created part of it. The re-run may only
     * push to people who really just got a new row -- pushing again to old recipients bothers them
     * twice.
     */
    @Test
    void should_push_only_to_recipients_whose_notification_was_newly_created() {
        UUID existing = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();

        // doAnswer rather than when(...): when() would really call saveIfAbsent(any()) on the mock
        // already stubbed in setUp, and the old answer receives null as the argument and throws an
        // NPE before it can re-stub.
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            return existing.equals(notification.getUserId()) ? Optional.empty() : Optional.of(withId(notification));
        }).when(notificationRepository).saveIfAbsent(any());

        consumer.consume(record(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED,
            new ExamBlueprintVersionPublishedEvent(
                List.of(existing, fresh), "BP-01", "Blueprint Toán 12", blueprintId, versionId)), ack);

        verify(notificationDeviceRepository, times(1)).findByUserId(fresh);
        verify(notificationDeviceRepository, never()).findByUserId(existing);
        verify(ack).acknowledge();
    }

    /** Trường không còn admin nào đang hoạt động: bỏ qua, không đẩy vào vòng retry. */
    @Test
    void should_skip_event_without_recipients_instead_of_failing() {
        consumer.consume(record(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED,
            new ExamBlueprintVersionPublishedEvent(
                List.of(), "BP-01", "Blueprint Toán 12", blueprintId, versionId)), ack);

        verify(notificationRepository, never()).saveIfAbsent(any());
        verifyNoInteractions(pushNotificationPort);
        verify(processedEventRepository).save(any());
        verify(ack).acknowledge();
    }

    @Test
    void should_not_map_events_for_users_without_devices() {
        assertThat(NotificationCategory.isMapped(EventTypeConstant.USER_CREATED)).isFalse();
        assertThat(NotificationCategory.isMapped(EventTypeConstant.REGISTER_FORM_REJECTED)).isFalse();
    }

    /** The DB row is the durable record, so it must be written before touching FCM. */
    @Test
    void should_persist_notification_before_pushing() {
        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        InOrder order = inOrder(notificationRepository, pushNotificationPort);
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

    /** The push must go through the executor, and must not run directly on the consumer thread. */
    @Test
    void should_dispatch_push_through_the_executor() {
        AtomicInteger submitted = new java.util.concurrent.atomic.AtomicInteger();
        consumer = newConsumer(task -> {
            submitted.incrementAndGet();
            task.run();
        });

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        assertThat(submitted.get()).isEqualTo(1);
        verify(pushNotificationPort).send(any(), anyList());
    }

    /**
     * Hàng đợi đầy là chuyện bình thường khi FCM chậm. Thông báo đã nằm trong DB nên
     * message phải được coi là xử lý xong -- không được để RejectedExecutionException lan
     * ra và kéo cả message vào vòng retry cho một công việc đã hoàn thành.
     */
    @Test
    void should_still_ack_when_push_queue_rejects() {
        consumer = newConsumer(task -> {
            throw new RejectedExecutionException("queue full");
        });

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        verify(notificationRepository).saveIfAbsent(any());
        verify(processedEventRepository).save(any());
        verify(ack).acknowledge();
    }

    /** Lỗi khi push chạy trên luồng pool không được thoát ra ngoài task. */
    @Test
    void should_not_let_push_failure_escape_the_task() {
        when(notificationDeviceRepository.findByUserId(any()))
            .thenThrow(new IllegalStateException("mất kết nối DB"));

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

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

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("eventType", EventTypeConstant.EXAM_RESULT_RELEASED)
            .containsEntry("target", NotificationTarget.EXAM_RESULT_DETAIL.name())
            .containsKey("candidateResultId");
    }

    /**
     * The student's result screen receives sessionId, not candidateResultId. Before the event
     * carried this extra field, the score notification could only open the exam list.
     */
    @Test
    void should_carry_session_id_and_exam_kind_for_result_notifications() {
        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.EXAM_RESULT_DETAIL.name())
            .containsEntry("sessionId", sessionId.toString())
            .containsEntry("examKind", ExamKind.CENTRALIZED.name())
            // The result screen on the Flutter side receives examName to build the title: it loads
            // the score by sessionId itself but has no other source for the exam name.
            .containsEntry("examName", "Kỳ thi giữa kỳ");
    }

    /**
     * The grading screen of a class test sits under examId, so the deadline reminder must carry it.
     */
    @Test
    void should_carry_exam_id_and_kind_for_grading_notifications() {
        consumer.consume(record(EventTypeConstant.GRADING_DEADLINE_REMINDER,
            new GradingDeadlineReminderPayloadV1(UUID.randomUUID(), userId, "Kỳ thi giữa kỳ", "FIRST",
                Instant.parse("2026-09-01T03:00:00Z"), examId, ExamKind.CLASS_TEST)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.TEACHER_GRADING_TASK.name())
            .containsEntry("examId", examId.toString())
            .containsEntry("examKind", ExamKind.CLASS_TEST.name());
    }

    /**
     * Assigning a grading task takes the teacher to EXACTLY their own assignment, so it reuses the
     * deadline reminder's target -- no new target is needed, this is where the target table pays
     * off.
     */
    @Test
    void should_route_a_new_assignment_to_the_teacher_grading_task() {
        consumer.consume(record(EventTypeConstant.GRADING_ASSIGNMENT_OPENED,
            new GradingAssignmentOpenedPayloadV1(
                UUID.randomUUID(), userId, examId, "Kiểm tra 15 phút", ExamKind.CLASS_TEST,
                "INITIAL", "Trần Quang Thiên", null)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.TEACHER_GRADING_TASK.name())
            .containsKey("assignmentId")
            .containsEntry("examId", examId.toString());
    }

    /**
     * The submission label was chosen by the anonymity rule AT PUBLISH TIME; the consumer only
     * displays it again. For a centralized exam it is the submission code, and the consumer must
     * not derive it again from examKind.
     */
    @Test
    void should_show_the_candidate_label_verbatim_in_the_body() {
        consumer.consume(record(EventTypeConstant.GRADING_ASSIGNMENT_OPENED,
            new GradingAssignmentOpenedPayloadV1(
                UUID.randomUUID(), userId, examId, "Kỳ thi giữa kỳ", ExamKind.CENTRALIZED,
                "SPOT_CHECK", "A2041F3C", null)), ack);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getBody())
            .contains("Kỳ thi giữa kỳ")
            .contains("Hậu kiểm")
            .contains("A2041F3C");
    }

    /**
     * A manual-grading reminder leads to the grading queue of the WHOLE exam, so it only needs
     * examId + kind -- there is no single submission id here, and there should not be: one exam is
     * hundreds of submissions.
     */
    @Test
    void should_carry_exam_id_and_kind_for_human_grading_reminders() {
        consumer.consume(record(EventTypeConstant.EXAM_HUMAN_GRADING_REQUIRED,
            new ExamHumanGradingRequiredPayloadV1(
                List.of(userId), examId, "Kỳ thi giữa kỳ", ExamKind.CLASS_TEST, 7)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.EXAM_HUMAN_GRADING_REQUIRED.name())
            .containsEntry("examId", examId.toString())
            .containsEntry("examKind", ExamKind.CLASS_TEST.name());
    }

    /**
     * The number of submissions still waiting is in the readable content part, not in the
     * navigation key.
     */
    @Test
    void should_put_the_pending_count_in_the_notification_body() {
        consumer.consume(record(EventTypeConstant.EXAM_HUMAN_GRADING_REQUIRED,
            new ExamHumanGradingRequiredPayloadV1(
                List.of(userId), examId, "Kỳ thi giữa kỳ", ExamKind.CENTRALIZED, 7)), ack);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Kỳ thi giữa kỳ").contains("7");
    }

    /**
     * No route looks up a blueprint by code, so the code stays in the display part while navigation
     * uses the id. Keep versionId too: the event is about one specific version that was just
     * published.
     */
    @Test
    void should_navigate_blueprint_notifications_by_id_not_by_code() {
        consumer.consume(record(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED,
            new ExamBlueprintVersionPublishedEvent(
                List.of(userId), "BP-01", "Blueprint Toán 12", blueprintId, versionId)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("blueprintId", blueprintId.toString())
            .containsEntry("versionId", versionId.toString())
            .doesNotContainKey("blueprintCode");
    }

    /**
     * Old events still within Kafka retention do not have the newly added fields. They must be
     * entirely absent from the map rather than becoming the string "null" -- the client
     * distinguishes "no key" from "a key with a meaningless value", and the string "null" would
     * leak into a URL.
     */
    @Test
    void should_omit_navigation_keys_that_the_event_does_not_carry() {
        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED,
            new ExamResultReleasedPayloadV1(
                UUID.randomUUID(), userId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"), null, null)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .doesNotContainKey("sessionId")
            .doesNotContainKey("examKind")
            .containsEntry("target", NotificationTarget.EXAM_RESULT_DETAIL.name());
        assertThat(captor.getValue().data().values()).doesNotContain("null");
    }

    /**
     * Without the id, a user tapping the notification tray can only open the list: the push data is
     * everything the client holds in that branch, {@code myNotifications} has not been called yet.
     */
    @Test
    void should_carry_saved_notification_id_into_push_data() {
        UUID savedId = UUID.randomUUID();
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(savedId);
            return Optional.of(notification);
        }).when(notificationRepository).saveIfAbsent(any());

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data()).containsEntry("notificationId", savedId.toString());
    }

    /**
     * The reverse of the test above: the payload column must NOT contain notificationId. The client
     * reading the list already has each row's id, and writing it here would only duplicate data in
     * a column that is never cleaned up.
     */
    @Test
    void should_not_persist_notification_id_into_the_payload_column() {
        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getPayload()).doesNotContain("notificationId");
    }

    /**
     * A null id means the adapter did not return the primary key. The push must still go out -- a
     * notification that opens the list is far better than a notification that never arrives.
     */
    @Test
    void should_still_push_without_deep_link_when_saved_row_has_no_id() {
        doAnswer(invocation -> Optional.of(invocation.getArgument(0)))
            .when(notificationRepository).saveIfAbsent(any());

        consumer.consume(record(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .doesNotContainKey("notificationId")
            .containsEntry("target", NotificationTarget.EXAM_RESULT_DETAIL.name());
        verify(ack).acknowledge();
    }

    /**
     * Cùng lý do với {@code should_cover_exactly_the_mapped_event_types}: category và target
     * là hai bảng riêng trên cùng một tập eventType, thiếu một bên thì renderDrafts ném giữa
     * chừng và message rơi vào DLT. Lưới an toàn thật nằm ở NotificationCategoryMappingValidator.
     */
    @Test
    void should_map_a_target_for_every_event_type_that_has_a_category() {
        assertThat(NotificationTarget.mappedEventTypes())
            .containsExactlyInAnyOrderElementsOf(NotificationCategory.mappedEventTypes());
    }

    /**
     * Hai event dưới đây cùng mang {@code assignmentId} nhưng gửi cho hai vai trò khác nhau,
     * nên phải là hai target khác nhau -- client đoán theo tên khoá sẽ đẩy admin vào màn hình
     * của giáo viên. Đây chính là lý do target tồn tại thay vì để client tự suy từ payload.
     */
    @Test
    void should_separate_targets_for_events_sharing_an_id_key_across_roles() {
        assertThat(NotificationTarget.of(EventTypeConstant.GRADING_DEADLINE_REMINDER))
            .isEqualTo(NotificationTarget.TEACHER_GRADING_TASK);
        assertThat(NotificationTarget.of(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED))
            .isEqualTo(NotificationTarget.ADMIN_GRADING_ASSIGNMENT);

        assertThat(NotificationTarget.of(EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT))
            .isEqualTo(NotificationTarget.SCHOOL_BILLING_OVERVIEW);
        assertThat(NotificationTarget.of(EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED))
            .isEqualTo(NotificationTarget.SYSTEM_SCHOOL_ATTENTION);
    }

    // --- helpers ---------------------------------------------------------------

    private NotificationPushedEventConsumer newConsumer(Executor withExecutor) {
        return new NotificationPushedEventConsumer(
            notificationRepository,
            notificationDeviceRepository,
            notificationPreferenceRepository,
            processedEventRepository,
            pushNotificationPort,
            jsonMapper,
            withExecutor);
    }

    private record TestCase(String eventType, Object payload) {}

    private List<TestCase> allEventTypes() {
        String examName = "Kỳ thi giữa kỳ";
        UUID id = UUID.randomUUID();
        return List.of(
            new TestCase(EventTypeConstant.EXAM_RESULT_RELEASED, releasedPayload()),
            new TestCase(EventTypeConstant.EXAM_RESULT_REGRADED, new ExamResultRegradedPayloadV1(
                id, userId, examName, "FIRST", new BigDecimal("7.0"), new BigDecimal("8.0"),
                sessionId, ExamKind.CENTRALIZED)),
            new TestCase(EventTypeConstant.EXAM_RESULT_INVALIDATED, new ExamResultInvalidatedPayloadV1(
                id, userId, examName, "Gian lận", sessionId, ExamKind.CENTRALIZED)),
            new TestCase(EventTypeConstant.EXAM_RESULT_INVALID_CLEARED, new ExamResultInvalidClearedPayloadV1(
                id, userId, examName, "Đã xác minh lại", sessionId, ExamKind.CENTRALIZED)),
            new TestCase(EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED, new ExamResultOutcomeDecidedPayloadV1(
                id, userId, examName, "PASSED", new BigDecimal("8.0"), sessionId, ExamKind.CENTRALIZED)),

            new TestCase(EventTypeConstant.EXAM_APPEAL_PUBLISHED, new ExamAppealPublishedPayloadV1(
                id, userId, examName, new BigDecimal("6.0"), new BigDecimal("7.5"))),
            new TestCase(EventTypeConstant.EXAM_APPEAL_REJECTED, new ExamAppealRejectedPayloadV1(
                id, userId, examName, "Không đủ căn cứ")),
            new TestCase(EventTypeConstant.EXAM_APPEAL_APPROVED, new ExamAppealApprovedPayloadV1(
                id, userId, examName, Instant.parse("2026-09-01T03:00:00Z"))),

            new TestCase(EventTypeConstant.GRADING_DEADLINE_REMINDER, new GradingDeadlineReminderPayloadV1(
                id, userId, examName, "FIRST", Instant.parse("2026-09-01T03:00:00Z"),
                examId, ExamKind.CENTRALIZED)),
            // The recipient is assignedBy (the admin who assigned the work), not teacherId.
            new TestCase(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, new GradingAssignmentDeclinedPayloadV1(
                id, id, UUID.randomUUID(), userId, examName, "Bận lịch coi thi",
                examId, ExamKind.CENTRALIZED)),

            new TestCase(EventTypeConstant.GRADING_ASSIGNMENT_OPENED,
                new GradingAssignmentOpenedPayloadV1(
                    id, userId, examId, examName, ExamKind.CLASS_TEST, "INITIAL",
                    "Trần Quang Thiên", Instant.parse("2026-09-01T03:00:00Z"))),

            new TestCase(EventTypeConstant.EXAM_HUMAN_GRADING_REQUIRED,
                new ExamHumanGradingRequiredPayloadV1(
                    List.of(userId), examId, examName, ExamKind.CENTRALIZED, 3)),

            // Two fan-out events: only one recipient on purpose here so the assertion loop above
            // can be shared. The multi-recipient behavior has its own test below.
            new TestCase(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED,
                new ExamBlueprintVersionPublishedEvent(
                    List.of(userId), "BP-01", "Blueprint Toán 12", blueprintId, versionId)),
            new TestCase(EventTypeConstant.INVOICE_PAID, new InvoicePaidPayloadV1(
                List.of(userId), id, id, id, "INV-001", new BigDecimal("500000"),
                Instant.parse("2026-09-01T03:00:00Z"), InvoiceSourceType.SUBSCRIPTION)),

            // Three new fan-out events, also with only one recipient on purpose for the assertion
            // loop above.
            new TestCase(EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, new SchoolDebtCapExceededPayloadV1(
                List.of(userId), id, id, QuotaType.EXAM, new BigDecimal("30"), new BigDecimal("20"),
                Instant.parse("2026-09-01T03:00:00Z"))),
            new TestCase(EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, new SchoolLockedDueToDebtPayloadV1(
                List.of(userId), id, id, Instant.parse("2026-09-01T03:00:00Z"))),
            new TestCase(EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, new SchoolQuotaUsageWarningPayloadV1(
                List.of(userId), id, id, QuotaType.EXAM, new BigDecimal("100"), new BigDecimal("75"),
                Instant.parse("2026-09-01T03:00:00Z"))),
            new TestCase(EventTypeConstant.SCHOOL_DEBT_CLEARED, new SchoolDebtClearedPayloadV1(
                List.of(userId), id, id, Instant.parse("2026-09-01T03:00:00Z"))),
            new TestCase(EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, new SchoolSubscriptionSuspendedPayloadV1(
                List.of(userId), id, id, "Phát hiện gian lận", Instant.parse("2026-09-01T03:00:00Z"))),
            new TestCase(EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED, new SchoolSubscriptionUnsuspendedPayloadV1(
                List.of(userId), id, id, Instant.parse("2026-09-01T03:00:00Z")))
        );
    }

    /**
     * saveIfAbsent thật trả về dòng ĐÃ ghi, tức là đã có khoá chính (xem
     * NotificationRepositoryImpl). Mock phải bắt chước đúng chi tiết đó: thiếu id thì nhánh
     * gắn notificationId vào push data không bao giờ chạy, và test sẽ xanh trong khi thực
     * tế client không mở được đúng mục.
     */
    private Notification withId(Notification notification) {
        notification.setId(UUID.randomUUID());
        return notification;
    }

    private ExamResultReleasedPayloadV1 releasedPayload() {
        return new ExamResultReleasedPayloadV1(
            UUID.randomUUID(), userId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"),
            sessionId, ExamKind.CENTRALIZED);
    }

    private NotificationDevice device(String installationId) {
        return new NotificationDevice(
            userId, "device-1", NotificationDevicePlatform.WEB, installationId, Instant.now(), Instant.now());
    }

    private ConsumerRecord<String, String> record(String eventType, Object payload) {
        String value = jsonMapper.writeValueAsString(payload);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("vox.exam-result-lifecycle.v1", 0, 0L, "key", value);
        record.headers().add("eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
