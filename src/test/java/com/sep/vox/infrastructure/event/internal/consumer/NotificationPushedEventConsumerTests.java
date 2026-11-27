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
import com.sep.vox.application.event.AssessmentAppealApprovedPayloadV1;
import com.sep.vox.application.event.AssessmentAppealPublishedPayloadV1;
import com.sep.vox.application.event.AssessmentAppealRejectedPayloadV1;
import com.sep.vox.application.event.AssessmentBlueprintVersionPublishedEvent;
import com.sep.vox.application.event.AssessmentHumanGradingRequiredPayloadV1;
import com.sep.vox.application.event.AssessmentResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.AssessmentResultInvalidatedPayloadV1;
import com.sep.vox.application.event.AssessmentResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.event.AssessmentResultRegradedPayloadV1;
import com.sep.vox.application.event.AssessmentResultReleasedPayloadV1;
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
import com.sep.vox.domain.model.assessment.AssessmentKind;
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
     * Runs the task right on the calling thread. This is exactly why the consumer takes an {@link Executor} instead
     * of {@code ThreadPoolTaskExecutor} or {@code @Async}: every push assertion below
     * stays deterministic, needs no await and cannot be flaky.
     */
    private Executor executor;

    private UUID userId;

    /** The two navigation keys that the target screen needs besides the id of the entity itself. */
    private UUID sessionId;
    private UUID assessmentId;
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
        assessmentId = UUID.randomUUID();
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
                .as("recipient of %s", testCase.eventType())
                .isEqualTo(userId);
            assertThat(saved.getTitle())
                .as("title of %s", testCase.eventType())
                .isNotBlank();
            assertThat(saved.getEventType()).isEqualTo(testCase.eventType());
            // The payload is the only thing the client has to know which screen to open, so every
            // eventType must write its target into it -- no notification should be one that "goes
            // nowhere when tapped".
            assertThat(saved.getPayload())
                .as("target in the payload of %s", testCase.eventType())
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

        consumer.consume(record(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED,
            new AssessmentBlueprintVersionPublishedEvent(
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

        consumer.consume(record(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED,
            new AssessmentBlueprintVersionPublishedEvent(
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

        consumer.consume(record(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED,
            new AssessmentBlueprintVersionPublishedEvent(
                List.of(existing, fresh), "BP-01", "Blueprint Toán 12", blueprintId, versionId)), ack);

        verify(notificationDeviceRepository, times(1)).findByUserId(fresh);
        verify(notificationDeviceRepository, never()).findByUserId(existing);
        verify(ack).acknowledge();
    }

    /** The school has no active admin left: skip, do not push into the retry loop. */
    @Test
    void should_skip_event_without_recipients_instead_of_failing() {
        consumer.consume(record(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED,
            new AssessmentBlueprintVersionPublishedEvent(
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
        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        InOrder order = inOrder(notificationRepository, pushNotificationPort);
        order.verify(notificationRepository).saveIfAbsent(any());
        order.verify(pushNotificationPort).send(any(), anyList());
    }

    @Test
    void should_still_persist_when_user_has_no_device() {
        when(notificationDeviceRepository.findByUserId(any())).thenReturn(List.of());

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        verify(notificationRepository).saveIfAbsent(any());
        verify(pushNotificationPort, never()).send(any(), anyList());
        verify(processedEventRepository).save(any());
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_push_when_preference_disabled() {
        when(notificationPreferenceRepository.findByUserIdAndCategory(any(), any()))
            .thenReturn(Optional.of(NotificationPreference.builder()
                    .userId(userId)
                    .category(NotificationCategory.ASSESSMENT_RESULT)
                    .pushEnabled(false)
                    .emailEnabled(true)
                    .updatedAt(Instant.now())
                    .build()));

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        verify(notificationRepository).saveIfAbsent(any());
        verify(pushNotificationPort, never()).send(any(), anyList());
    }

    @Test
    void should_push_by_default_when_preference_row_is_absent() {
        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        verify(pushNotificationPort).send(any(), anyList());
    }

    /** Only the stale group is deleted; a transient error must keep the device. */
    @Test
    void should_delete_only_stale_installation_ids() {
        when(pushNotificationPort.send(any(), anyList()))
            .thenReturn(new PushDispatchResult(0, List.of("fid-dead"), List.of("fid-flaky")));

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(notificationDeviceRepository).deleteByInstallationIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactly("fid-dead");
    }

    @Test
    void should_not_push_twice_when_notification_already_exists() {
        // doReturn instead of when(...): the mock was already stubbed with thenAnswer in setUp, so
        // when(mock.saveIfAbsent(any())) would actually call the old answer with a null argument.
        doReturn(Optional.empty()).when(notificationRepository).saveIfAbsent(any());

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

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

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        assertThat(submitted.get()).isEqualTo(1);
        verify(pushNotificationPort).send(any(), anyList());
    }

    /**
     * A full queue is normal when FCM is slow. The notification is already in the DB so the
     * message must be treated as fully processed -- RejectedExecutionException must not spread
     * out and pull the message into the retry loop for work that is already done.
     */
    @Test
    void should_still_ack_when_push_queue_rejects() {
        consumer = newConsumer(task -> {
            throw new RejectedExecutionException("queue full");
        });

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        verify(notificationRepository).saveIfAbsent(any());
        verify(processedEventRepository).save(any());
        verify(ack).acknowledge();
    }

    /** An error while pushing on the pool thread must not escape the task. */
    @Test
    void should_not_let_push_failure_escape_the_task() {
        when(notificationDeviceRepository.findByUserId(any()))
            .thenThrow(new IllegalStateException("DB connection lost"));

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        verify(processedEventRepository).save(any());
        verify(ack).acknowledge();
    }

    @Test
    void should_skip_already_processed_event() {
        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(), any())).thenReturn(true);

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationPort);
        verify(ack).acknowledge();
    }

    @Test
    void should_carry_event_type_into_push_data_for_deep_linking() {
        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("eventType", EventTypeConstant.ASSESSMENT_RESULT_RELEASED)
            .containsEntry("target", NotificationTarget.ASSESSMENT_RESULT_DETAIL.name())
            .containsKey("candidateResultId");
    }

    /**
     * The student's result screen receives sessionId, not candidateResultId. Before the event
     * carried this extra field, the score notification could only open the assessment list.
     */
    @Test
    void should_carry_session_id_and_assessment_kind_for_result_notifications() {
        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.ASSESSMENT_RESULT_DETAIL.name())
            .containsEntry("sessionId", sessionId.toString())
            .containsEntry("assessmentKind", AssessmentKind.CENTRALIZED.name())
            // The result screen on the Flutter side receives assessmentName to build the title: it loads
            // the score by sessionId itself but has no other source for the assessment name.
            .containsEntry("assessmentName", "Kỳ thi giữa kỳ");
    }

    /**
     * The grading screen of a class test sits under assessmentId, so the deadline reminder must carry it.
     */
    @Test
    void should_carry_assessment_id_and_kind_for_grading_notifications() {
        consumer.consume(record(EventTypeConstant.GRADING_DEADLINE_REMINDER,
            new GradingDeadlineReminderPayloadV1(UUID.randomUUID(), userId, "Kỳ thi giữa kỳ", "FIRST",
                Instant.parse("2026-09-01T03:00:00Z"), assessmentId, AssessmentKind.CLASS_TEST)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.TEACHER_GRADING_TASK.name())
            .containsEntry("assessmentId", assessmentId.toString())
            .containsEntry("assessmentKind", AssessmentKind.CLASS_TEST.name());
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
                UUID.randomUUID(), userId, assessmentId, "Kiểm tra 15 phút", AssessmentKind.CLASS_TEST,
                "INITIAL", "Trần Quang Thiên", null)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.TEACHER_GRADING_TASK.name())
            .containsKey("assignmentId")
            .containsEntry("assessmentId", assessmentId.toString());
    }

    /**
     * The submission label was chosen by the anonymity rule AT PUBLISH TIME; the consumer only
     * displays it again. For a centralized assessment it is the submission code, and the consumer must
     * not derive it again from assessmentKind.
     */
    @Test
    void should_show_the_candidate_label_verbatim_in_the_body() {
        consumer.consume(record(EventTypeConstant.GRADING_ASSIGNMENT_OPENED,
            new GradingAssignmentOpenedPayloadV1(
                UUID.randomUUID(), userId, assessmentId, "Kỳ thi giữa kỳ", AssessmentKind.CENTRALIZED,
                "SPOT_CHECK", "A2041F3C", null)), ack);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getBody())
            .contains("Kỳ thi giữa kỳ")
            .contains("Spot check")
            .contains("A2041F3C");
    }

    /**
     * A manual-grading reminder leads to the grading queue of the WHOLE assessment, so it only needs
     * assessmentId + kind -- there is no single submission id here, and there should not be: one assessment is
     * hundreds of submissions.
     */
    @Test
    void should_carry_assessment_id_and_kind_for_human_grading_reminders() {
        consumer.consume(record(EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED,
            new AssessmentHumanGradingRequiredPayloadV1(
                List.of(userId), assessmentId, "Kỳ thi giữa kỳ", AssessmentKind.CLASS_TEST, 7)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .containsEntry("target", NotificationTarget.ASSESSMENT_HUMAN_GRADING_REQUIRED.name())
            .containsEntry("assessmentId", assessmentId.toString())
            .containsEntry("assessmentKind", AssessmentKind.CLASS_TEST.name());
    }

    /**
     * The number of submissions still waiting is in the readable content part, not in the
     * navigation key.
     */
    @Test
    void should_put_the_pending_count_in_the_notification_body() {
        consumer.consume(record(EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED,
            new AssessmentHumanGradingRequiredPayloadV1(
                List.of(userId), assessmentId, "Kỳ thi giữa kỳ", AssessmentKind.CENTRALIZED, 7)), ack);

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
        consumer.consume(record(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED,
            new AssessmentBlueprintVersionPublishedEvent(
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
        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED,
            new AssessmentResultReleasedPayloadV1(
                UUID.randomUUID(), userId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"), null, null)), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .doesNotContainKey("sessionId")
            .doesNotContainKey("assessmentKind")
            .containsEntry("target", NotificationTarget.ASSESSMENT_RESULT_DETAIL.name());
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

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

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
        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

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

        consumer.consume(record(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()), ack);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushNotificationPort).send(captor.capture(), anyList());
        assertThat(captor.getValue().data())
            .doesNotContainKey("notificationId")
            .containsEntry("target", NotificationTarget.ASSESSMENT_RESULT_DETAIL.name());
        verify(ack).acknowledge();
    }

    /**
     * Same reason as {@code should_cover_exactly_the_mapped_event_types}: category and target
     * are two separate tables over the same set of eventTypes, and if one side is missing renderDrafts throws midway
     * and the message falls into the DLT. The real safety net is in NotificationCategoryMappingValidator.
     */
    @Test
    void should_map_a_target_for_every_event_type_that_has_a_category() {
        assertThat(NotificationTarget.mappedEventTypes())
            .containsExactlyInAnyOrderElementsOf(NotificationCategory.mappedEventTypes());
    }

    /**
     * The two events below both carry {@code assignmentId} but are sent to two different roles,
     * so they must be two different targets -- a client guessing by key name would push an admin onto the
     * teacher's screen. This is exactly why the target exists instead of letting the client infer it from the payload.
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
        String assessmentName = "Kỳ thi giữa kỳ";
        UUID id = UUID.randomUUID();
        return List.of(
            new TestCase(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, releasedPayload()),
            new TestCase(EventTypeConstant.ASSESSMENT_RESULT_REGRADED, new AssessmentResultRegradedPayloadV1(
                id, userId, assessmentName, "FIRST", new BigDecimal("7.0"), new BigDecimal("8.0"),
                sessionId, AssessmentKind.CENTRALIZED)),
            new TestCase(EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED, new AssessmentResultInvalidatedPayloadV1(
                id, userId, assessmentName, "Gian lận", sessionId, AssessmentKind.CENTRALIZED)),
            new TestCase(EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED, new AssessmentResultInvalidClearedPayloadV1(
                id, userId, assessmentName, "Đã xác minh lại", sessionId, AssessmentKind.CENTRALIZED)),
            new TestCase(EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED, new AssessmentResultOutcomeDecidedPayloadV1(
                id, userId, assessmentName, "PASSED", new BigDecimal("8.0"), sessionId, AssessmentKind.CENTRALIZED)),

            new TestCase(EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED, new AssessmentAppealPublishedPayloadV1(
                id, userId, assessmentName, new BigDecimal("6.0"), new BigDecimal("7.5"))),
            new TestCase(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, new AssessmentAppealRejectedPayloadV1(
                id, userId, assessmentName, "Không đủ căn cứ")),
            new TestCase(EventTypeConstant.ASSESSMENT_APPEAL_APPROVED, new AssessmentAppealApprovedPayloadV1(
                id, userId, assessmentName, Instant.parse("2026-09-01T03:00:00Z"))),

            new TestCase(EventTypeConstant.GRADING_DEADLINE_REMINDER, new GradingDeadlineReminderPayloadV1(
                id, userId, assessmentName, "FIRST", Instant.parse("2026-09-01T03:00:00Z"),
                assessmentId, AssessmentKind.CENTRALIZED)),
            // The recipient is assignedBy (the admin who assigned the work), not teacherId.
            new TestCase(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, new GradingAssignmentDeclinedPayloadV1(
                id, id, UUID.randomUUID(), userId, assessmentName, "Bận lịch coi thi",
                assessmentId, AssessmentKind.CENTRALIZED)),

            new TestCase(EventTypeConstant.GRADING_ASSIGNMENT_OPENED,
                new GradingAssignmentOpenedPayloadV1(
                    id, userId, assessmentId, assessmentName, AssessmentKind.CLASS_TEST, "INITIAL",
                    "Trần Quang Thiên", Instant.parse("2026-09-01T03:00:00Z"))),

            new TestCase(EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED,
                new AssessmentHumanGradingRequiredPayloadV1(
                    List.of(userId), assessmentId, assessmentName, AssessmentKind.CENTRALIZED, 3)),

            // Two fan-out events: only one recipient on purpose here so the assertion loop above
            // can be shared. The multi-recipient behavior has its own test below.
            new TestCase(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED,
                new AssessmentBlueprintVersionPublishedEvent(
                    List.of(userId), "BP-01", "Blueprint Toán 12", blueprintId, versionId)),
            new TestCase(EventTypeConstant.INVOICE_PAID, new InvoicePaidPayloadV1(
                List.of(userId), id, id, id, "INV-001", new BigDecimal("500000"),
                Instant.parse("2026-09-01T03:00:00Z"), InvoiceSourceType.SUBSCRIPTION)),

            // Three new fan-out events, also with only one recipient on purpose for the assertion
            // loop above.
            new TestCase(EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, new SchoolDebtCapExceededPayloadV1(
                List.of(userId), id, id, QuotaType.ASSESSMENT, new BigDecimal("30"), new BigDecimal("20"),
                Instant.parse("2026-09-01T03:00:00Z"))),
            new TestCase(EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, new SchoolLockedDueToDebtPayloadV1(
                List.of(userId), id, id, Instant.parse("2026-09-01T03:00:00Z"))),
            new TestCase(EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, new SchoolQuotaUsageWarningPayloadV1(
                List.of(userId), id, id, QuotaType.ASSESSMENT, new BigDecimal("100"), new BigDecimal("75"),
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
     * The real saveIfAbsent returns the row that WAS written, meaning it already has a primary key (see
     * NotificationRepositoryImpl). The mock must imitate exactly that detail: without an id the branch
     * attaching notificationId to the push data never runs, and the test would be green while in
     * reality the client cannot open the right entry.
     */
    private Notification withId(Notification notification) {
        notification.setId(UUID.randomUUID());
        return notification;
    }

    private AssessmentResultReleasedPayloadV1 releasedPayload() {
        return new AssessmentResultReleasedPayloadV1(
            UUID.randomUUID(), userId, "Kỳ thi giữa kỳ", new BigDecimal("8.5"),
            sessionId, AssessmentKind.CENTRALIZED);
    }

    private NotificationDevice device(String installationId) {
        return NotificationDevice.builder()
                .userId(userId)
                .deviceId("device-1")
                .platform(NotificationDevicePlatform.WEB)
                .installationId(installationId)
                .createdAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build();
    }

    private ConsumerRecord<String, String> record(String eventType, Object payload) {
        String value = jsonMapper.writeValueAsString(payload);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("vox.assessment-result-lifecycle.v1", 0, 0L, "key", value);
        record.headers().add("eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
