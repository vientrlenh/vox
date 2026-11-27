package com.sep.vox.infrastructure.event.internal.consumer;

import com.sep.vox.application.response.output.PushDispatchResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.application.common.DateMapper;
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
import com.sep.vox.application.response.output.PushMessage;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationPreference;
import com.sep.vox.domain.model.notification.NotificationTarget;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.NotificationDeviceRepository;
import com.sep.vox.domain.repository.NotificationPreferenceRepository;
import com.sep.vox.domain.repository.NotificationRepository;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the three topics that the mail consumer also uses (with its own separate consumer group), plus the
 * assessment-blueprint-version topic that only notification reads.
 *
 * <p>The processing order is the most important part: <b>write the notifications table first, push afterwards</b>.
 * The DB row is the durable record -- a user who has not registered any device, or when
 * FCM is down, must still see the notification when opening the app. The push is only an attention layer on top; if it fails
 * it is skipped and does not drag the whole message into the retry loop.
 *
 * <p>That is also why this consumer parses the payloads of the business events themselves
 * ({@code AssessmentResultReleasedPayloadV1}...) and builds the title/body itself, instead of waiting for a ready-made
 * notification payload: messages on those topics have a business shape and not
 * a notification shape.
 *
 * <p>One event may produce several notification rows. The recipient list is always already in
 * the payload and is not queried again here -- see {@link #fanOut}.
 */
@Component
@Slf4j
public class NotificationPushedEventConsumer {

    private static final String CONSUMER_GROUP = "notification";

    /** {@code withZone} is mandatory: without it the notification shows the container's UTC time. */
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    /** DecimalFormat is NOT thread-safe, so this must be a ThreadLocal and not a shared field. */
    private static final ThreadLocal<DecimalFormat> AMOUNT_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN"))));

    private final NotificationRepository notificationRepository;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PushNotificationPort pushNotificationPort;
    private final JsonMapper jsonMapper;
    private final Executor executor;

    public NotificationPushedEventConsumer(
            NotificationRepository notificationRepository,
            NotificationDeviceRepository notificationDeviceRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ProcessedEventRepository processedEventRepository,
            PushNotificationPort pushNotificationPort,
            JsonMapper jsonMapper, @Qualifier("pushExecutor") Executor executor) {
        this.notificationRepository = notificationRepository;
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.processedEventRepository = processedEventRepository;
        this.pushNotificationPort = pushNotificationPort;
        this.jsonMapper = jsonMapper;
        this.executor = executor;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        kafkaTemplate = "outboxKafkaTemplate",
        // The retry topic name is derived only from the original topic name, not from the consumer group. Without these two
        // separate suffixes, mail and notification would share a retry topic, and a failed
        // message on the mail side would be reprocessed by notification.
        retryTopicSuffix = "-notif-retry",
        dltTopicSuffix = "-notif-dlt"
    )
    @KafkaListener(
        topics = {
            "${app.internal-event.kafka.consumer-groups.notification.topic.assessment-appeal}",
            "${app.internal-event.kafka.consumer-groups.notification.topic.assessment-result-lifecycle}",
            "${app.internal-event.kafka.consumer-groups.notification.topic.grading-assignment}",
            "${app.internal-event.kafka.consumer-groups.notification.topic.assessment-blueprint-version}",
            // A separate topic, NOT shared with grading-assignment: the mail consumer reads
            // that topic and throws on the unknown eventType. The manual-grading reminder is also deliberately shown only in the app.
            "${app.internal-event.kafka.consumer-groups.notification.topic.assessment-grading-review}",
            // FIX: InvoicePaid already had a handling case below and the BILLING category, but this exact
            // line was missing, so the consumer never received any message from the invoice topic -- the
            // in-app notification "Invoice paid" never ran (the mail still works
            // normally through InvoiceEmailConsumer, which has its own correct topic).
            "${app.internal-event.kafka.consumer-groups.notification.topic.invoice}",
            "${app.internal-event.kafka.consumer-groups.notification.topic.school-debt}"
        },
        groupId = "${app.internal-event.kafka.consumer-groups.notification.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory",
        // Overrides the shared config's auto-offset-reset: earliest. This is a new consumer group,
        // with no committed offset yet, so with earliest it would re-read the entire history still
        // within retention and fire a mass of pushes about old events on the very first deploy.
        properties = { "auto.offset.reset=latest" }
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event notification: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        notifyAndAcknowledge(record, eventId, ack);
    }

    private void notifyAndAcknowledge(ConsumerRecord<String, String> record, UUID eventId, Acknowledgment ack) {
        String eventType = KafkaEventHeaders.readEventType(record);

        if (!NotificationCategory.isMapped(eventType)) {
            skipUnmappedEventType(eventId, eventType);
            ack.acknowledge();
            return;
        }

        List<Draft> drafts = renderDrafts(eventType, eventId, record.value());

        if (drafts.isEmpty()) {
            skipWithoutRecipients(eventId, eventType);
            ack.acknowledge();
            return;
        }

        notifyRecipients(drafts, eventId, eventType);
        ack.acknowledge();
    }

    /**
     * An unknown eventType means a new event was added without declaring its category. Mark it processed
     * and skip: running the whole retry cycle cannot change the outcome, and letting it fall into the DLT
     * only creates noise. The real safety net is NotificationCategoryMappingValidator,
     * which blocks at startup.
     */
    private void skipUnmappedEventType(UUID eventId, String eventType) {
        log.warn("Skip notification, eventType not mapped to a category: eventId={}, eventType={}",
            eventId, eventType);
        markProcessed(eventId);
    }

    /**
     * The payload names no recipient. For a fan-out event that means no active
     * admin is left; for a single-recipient event the payload is missing an id. Neither
     * can be fixed by running it again.
     */
    private void skipWithoutRecipients(UUID eventId, String eventType) {
        log.warn("Skip notification, no recipient: eventId={}, eventType={}",
            eventId, eventType);
        markProcessed(eventId);
    }

    private void notifyRecipients(List<Draft> drafts, UUID eventId, String eventType) {
        List<Pushable> pushable = saveNotifications(drafts, eventId, eventType);

        if (pushable.isEmpty()) {
            // uk_notifications_user_event blocked everything: the message was fully processed before but
            // markProcessed had not run yet (crash between the two steps). Do not push a second time.
            log.info("Notification already exists for all {} recipients, skipping push: eventId={}",
                drafts.size(), eventId);
            markProcessed(eventId);
            return;
        }

        submitPushes(pushable, eventId);

        markProcessed(eventId);

        log.info("Notification created: eventId={}, eventType={}, recipients={}, pushable={}",
            eventId, eventType, drafts.size(), pushable.size());
    }

    private List<Pushable> saveNotifications(List<Draft> drafts, UUID eventId, String eventType) {
        // Durable step: write the table before anything else. One Instant for the whole batch so that every
        // recipient of an event has the same createdAt -- the pagination cursor sorts by id
        // so it is not affected, but a difference of a few dozen ms between rows of the same event only causes
        // confusion when reading logs.
        Instant now = Instant.now();
        List<Pushable> pushable = new ArrayList<Pushable>();
        for (Draft draft : drafts) {
            Optional<Notification> saved = saveDraft(draft, eventId, eventType, now);

            // The decision to push must be made PER RECIPIENT, not for the whole
            // message: a crash midway last time may have already created 30 of 50 rows, and this
            // time only the remaining 20 people deserve a push.
            if (saved.isPresent()) {
                pushable.add(new Pushable(draft, saved.get().getId()));
            }
        }
        return pushable;
    }

    private Optional<Notification> saveDraft(Draft draft, UUID eventId, String eventType, Instant now) {
        return notificationRepository.saveIfAbsent(Notification.builder()
                .userId(draft.userId())
                .eventId(eventId)
                .eventType(eventType)
                .title(draft.title())
                .body(draft.body())
                .payload(writePayload(draft.data()))
                .readAt(null)
                .createdAt(now)
                .build());
    }

    /**
     * Swallowing errors here and in {@link #pushAll} is INTENTIONAL, unlike the SmtpEmailSendingService case: the
     * notifications row has already been written, so losing one push only loses the alert layer, the
     * user still sees everything when opening the app. Letting the error propagate would rerun the whole retry cycle for
     * work that is actually already done.
     */
    private void submitPushes(List<Pushable> pushable, UUID eventId) {
        try {
            // The whole batch goes in ONE task, not one task per recipient: pushExecutor uses
            // AbortPolicy, so N tasks for one fan-out event would fill the queue and drop the
            // pushes of other events that are waiting.
            executor.execute(() -> pushAll(pushable, eventId));
        } catch (RejectedExecutionException e) {
            log.warn("Push queue is full, skipping push: eventId={}, recipients={}",
                eventId, pushable.size());
        }
    }

    private void pushAll(List<Pushable> pushable, UUID eventId) {
        for (Pushable entry : pushable) {
            try {
                pushBestEffort(entry, eventId);
            } catch (Exception e) {
                // Runs on a pool thread: without logging here the exception falls into the thread's uncaught
                // handler and never reaches the log file. Caught inside the loop so that one failing
                // recipient does not cut off the pushes of the remaining ones.
                log.error("Push failed outside consumer thread: eventId={}, userId={}",
                    eventId, entry.draft().userId(), e);
            }
        }
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        log.error("Notification event sent to the DLT: topic={}, partition={}, offset={}, payload={}",
            record.topic(), record.partition(), record.offset(), record.value());
    }

    /**
     * A push must never throw an error out: the notification row has already been written, and a message
     * that runs again only stops at the empty saveIfAbsent branch above rather than pushing again.
     */
    private void pushBestEffort(Pushable pushable, UUID eventId) {
        Draft draft = pushable.draft();

        if (!pushNotificationPort.isEnabled()) {
            return;
        }

        if (!pushEnabled(draft.userId(), draft.category())) {
            log.debug("The user has turned off push for group {}: userId={}", draft.category(), draft.userId());
            return;
        }

        List<String> installationIds = notificationDeviceRepository.findByUserId(draft.userId()).stream()
            .map(device -> device.getInstallationId())
            .toList();

        if (installationIds.isEmpty()) {
            log.debug("No device registered: userId={}", draft.userId());
            return;
        }

        sendPush(pushable, installationIds, eventId);
    }

    private void sendPush(Pushable pushable, List<String> installationIds, UUID eventId) {
        Draft draft = pushable.draft();
        PushDispatchResult result = pushNotificationPort.send(
            new PushMessage(draft.title(), draft.body(), pushData(pushable)), installationIds);

        // Only the stale group is deleted. The retryable group must be kept -- deleting on a transient error
        // would mean wiping the whole device table after a single FCM downtime.
        if (!result.staleInstallationIds().isEmpty()) {
            int removed = notificationDeviceRepository.deleteByInstallationIdIn(result.staleInstallationIds());
            log.info("Cleaned up {} devices that are no longer usable: eventId={}", removed, eventId);
        }

        log.debug("Push xong: eventId={}, success={}, stale={}, retryable={}",
            eventId, result.successCount(),
            result.staleInstallationIds().size(), result.retryableInstallationIds().size());
    }

    private boolean pushEnabled(UUID userId, NotificationCategory category) {
        return notificationPreferenceRepository.findByUserIdAndCategory(userId, category)
            .map(preference -> preference.isPushEnabled())
            // No row means the setting was never changed -> use the default.
            .orElse(NotificationPreference.DEFAULT_PUSH_ENABLED);
    }

    /**
     * @return one Draft per recipient. An empty list means the event has no
     *         recipient at all -- the caller skips the message instead of pushing it into retry.
     */
    private List<Draft> renderDrafts(String eventType, UUID eventId, String value) {
        IncomingEvent event = new IncomingEvent(value, eventType, eventId, NotificationCategory.of(eventType));

        return switch (eventType) {
            case EventTypeConstant.ASSESSMENT_RESULT_RELEASED, EventTypeConstant.ASSESSMENT_RESULT_REGRADED,
                EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED, EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED,
                EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED -> renderResultDrafts(event);
            case EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED, EventTypeConstant.ASSESSMENT_APPEAL_REJECTED,
                EventTypeConstant.ASSESSMENT_APPEAL_APPROVED -> renderAppealDrafts(event);
            case EventTypeConstant.GRADING_DEADLINE_REMINDER, EventTypeConstant.GRADING_ASSIGNMENT_DECLINED,
                EventTypeConstant.GRADING_ASSIGNMENT_OPENED, EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED ->
                renderGradingDrafts(event);
            case EventTypeConstant.INVOICE_PAID, EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED ->
                renderSchoolAdminDrafts(event);
            case EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT,
                EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, EventTypeConstant.SCHOOL_DEBT_CLEARED,
                EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED ->
                renderSchoolDrafts(event);
            default -> throw unhandledEventType(event);
        };
    }

    private IllegalStateException unhandledEventType(IncomingEvent event) {
        return new IllegalStateException(
            "Unhandled eventType: eventId=" + event.eventId() + ", eventType=" + event.eventType());
    }

    private List<Draft> renderResultDrafts(IncomingEvent event) {
        return switch (event.eventType()) {
            case EventTypeConstant.ASSESSMENT_RESULT_RELEASED -> renderResultReleased(event);
            case EventTypeConstant.ASSESSMENT_RESULT_REGRADED -> renderResultRegraded(event);
            case EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED -> renderResultInvalidated(event);
            case EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED -> renderResultInvalidCleared(event);
            case EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED -> renderResultOutcomeDecided(event);
            default -> throw unhandledEventType(event);
        };
    }

    private List<Draft> renderResultReleased(IncomingEvent event) {
        AssessmentResultReleasedPayloadV1 payload = parse(event, AssessmentResultReleasedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Your assessment score is available",
            "%s: %s points".formatted(orPlaceholder(payload.assessmentName()), formatScore(payload.totalScore())),
            resultNavigation(event, payload.candidateResultId(), payload.sessionId(),
                payload.assessmentKind(), payload.assessmentName()));
    }

    private List<Draft> renderResultRegraded(IncomingEvent event) {
        AssessmentResultRegradedPayloadV1 payload = parse(event, AssessmentResultRegradedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Your assessment score has changed",
            "%s: %s -> %s points".formatted(orPlaceholder(payload.assessmentName()),
                formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
            resultNavigation(event, payload.candidateResultId(), payload.sessionId(),
                payload.assessmentKind(), payload.assessmentName()));
    }

    private List<Draft> renderResultInvalidated(IncomingEvent event) {
        AssessmentResultInvalidatedPayloadV1 payload = parse(event, AssessmentResultInvalidatedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Your submission has been invalidated",
            "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
            resultNavigation(event, payload.candidateResultId(), payload.sessionId(),
                payload.assessmentKind(), payload.assessmentName()));
    }

    private List<Draft> renderResultInvalidCleared(IncomingEvent event) {
        AssessmentResultInvalidClearedPayloadV1 payload = parse(event, AssessmentResultInvalidClearedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Your submission has been reinstated",
            "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
            resultNavigation(event, payload.candidateResultId(), payload.sessionId(),
                payload.assessmentKind(), payload.assessmentName()));
    }

    private List<Draft> renderResultOutcomeDecided(IncomingEvent event) {
        AssessmentResultOutcomeDecidedPayloadV1 payload = parse(event, AssessmentResultOutcomeDecidedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Your final result",
            "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.outcome())),
            resultNavigation(event, payload.candidateResultId(), payload.sessionId(),
                payload.assessmentKind(), payload.assessmentName()));
    }

    private Map<String, String> resultNavigation(IncomingEvent event, UUID candidateResultId, UUID sessionId,
            AssessmentKind assessmentKind, String assessmentName) {
        return navigationData(event.eventType())
            .with("candidateResultId", candidateResultId)
            .with("sessionId", sessionId)
            .with("assessmentKind", assessmentKind)
            .with("assessmentName", assessmentName)
            .build();
    }

    private List<Draft> renderAppealDrafts(IncomingEvent event) {
        return switch (event.eventType()) {
            case EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED -> renderAppealPublished(event);
            case EventTypeConstant.ASSESSMENT_APPEAL_REJECTED -> renderAppealRejected(event);
            case EventTypeConstant.ASSESSMENT_APPEAL_APPROVED -> renderAppealApproved(event);
            default -> throw unhandledEventType(event);
        };
    }

    private List<Draft> renderAppealPublished(IncomingEvent event) {
        AssessmentAppealPublishedPayloadV1 payload = parse(event, AssessmentAppealPublishedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Appeal result published",
            "%s: %s -> %s points".formatted(orPlaceholder(payload.assessmentName()),
                formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
            navigationData(event.eventType()).with("appealId", payload.appealId()).build());
    }

    private List<Draft> renderAppealRejected(IncomingEvent event) {
        AssessmentAppealRejectedPayloadV1 payload = parse(event, AssessmentAppealRejectedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Appeal not accepted",
            "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
            navigationData(event.eventType()).with("appealId", payload.appealId()).build());
    }

    private List<Draft> renderAppealApproved(IncomingEvent event) {
        AssessmentAppealApprovedPayloadV1 payload = parse(event, AssessmentAppealApprovedPayloadV1.class);
        return one(payload.studentId(), event.category(),
            "Your appeal has been approved",
            "%s -- result expected by %s".formatted(
                orPlaceholder(payload.assessmentName()), formatDeadline(payload.deadline())),
            navigationData(event.eventType()).with("appealId", payload.appealId()).build());
    }

    private List<Draft> renderGradingDrafts(IncomingEvent event) {
        return switch (event.eventType()) {
            case EventTypeConstant.GRADING_DEADLINE_REMINDER -> renderGradingDeadlineReminder(event);
            case EventTypeConstant.GRADING_ASSIGNMENT_DECLINED -> renderGradingAssignmentDeclined(event);
            case EventTypeConstant.GRADING_ASSIGNMENT_OPENED -> renderGradingAssignmentOpened(event);
            case EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED -> renderHumanGradingRequired(event);
            default -> throw unhandledEventType(event);
        };
    }

    private List<Draft> renderGradingDeadlineReminder(IncomingEvent event) {
        GradingDeadlineReminderPayloadV1 payload = parse(event, GradingDeadlineReminderPayloadV1.class);
        return one(payload.teacherId(), event.category(),
            "Grading deadline approaching",
            "%s -- deadline %s".formatted(orPlaceholder(payload.assessmentName()),
                formatDeadline(payload.deadlineAt())),
            gradingNavigation(event, payload.assignmentId(), payload.assessmentId(), payload.assessmentKind()));
    }

    private List<Draft> renderGradingAssignmentDeclined(IncomingEvent event) {
        GradingAssignmentDeclinedPayloadV1 payload = parse(event, GradingAssignmentDeclinedPayloadV1.class);
        // The recipient is the admin who assigned the work, not the teacher who returned it.
        return one(payload.assignedBy(), event.category(),
            "A grading assignment was returned",
            "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
            gradingNavigation(event, payload.assignmentId(), payload.assessmentId(), payload.assessmentKind()));
    }

    private List<Draft> renderGradingAssignmentOpened(IncomingEvent event) {
        GradingAssignmentOpenedPayloadV1 payload = parse(event, GradingAssignmentOpenedPayloadV1.class);
        return one(payload.teacherId(), event.category(),
            "You have been assigned a grading task",
            // candidateLabel was already chosen by the anonymity rule at publish time: the student's name
            // for a class test, the attempt code for a centralized assessment. Do NOT re-derive it here.
            "%s - %s: %s".formatted(
                orPlaceholder(payload.assessmentName()),
                roundLabel(payload.roundType()),
                orPlaceholder(payload.candidateLabel())),
            gradingNavigation(event, payload.assignmentId(), payload.assessmentId(), payload.assessmentKind()));
    }

    private Map<String, String> gradingNavigation(IncomingEvent event, UUID assignmentId, UUID assessmentId,
            AssessmentKind assessmentKind) {
        return navigationData(event.eventType())
            .with("assignmentId", assignmentId)
            .with("assessmentId", assessmentId)
            .with("assessmentKind", assessmentKind)
            .build();
    }

    private List<Draft> renderHumanGradingRequired(IncomingEvent event) {
        AssessmentHumanGradingRequiredPayloadV1 payload = parse(event, AssessmentHumanGradingRequiredPayloadV1.class);
        return fanOut(payload.recipientIds(), event.category(),
            "Submissions waiting for AI score review",
            "%s: %d submissions need manual grading".formatted(
                orPlaceholder(payload.assessmentName()), payload.pendingCount()),
            navigationData(event.eventType())
                .with("assessmentId", payload.assessmentId())
                .with("assessmentKind", payload.assessmentKind())
                .build());
    }

    private List<Draft> renderSchoolAdminDrafts(IncomingEvent event) {
        return switch (event.eventType()) {
            case EventTypeConstant.INVOICE_PAID -> renderInvoicePaid(event);
            case EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED -> renderBlueprintVersionPublished(event);
            default -> throw unhandledEventType(event);
        };
    }

    private List<Draft> renderInvoicePaid(IncomingEvent event) {
        InvoicePaidPayloadV1 payload = parse(event, InvoicePaidPayloadV1.class);
        return fanOut(payload.schoolAdminIds(), event.category(),
            "Invoice paid",
            "Invoice #%s -- %s".formatted(orPlaceholder(payload.invoiceNumber()),
                formatAmount(payload.amount())),
            navigationData(event.eventType()).with("invoiceNumber", payload.invoiceNumber()).build());
    }

    // Same fan-out shape as InvoicePaid: when a blueprint is published, EVERY school
    // admin of the school needs to know.
    private List<Draft> renderBlueprintVersionPublished(IncomingEvent event) {
        AssessmentBlueprintVersionPublishedEvent payload =
            parse(event, AssessmentBlueprintVersionPublishedEvent.class);
        return fanOut(payload.schoolAdminIds(), event.category(),
            "Assessment blueprint is ready",
            "%s (%s)".formatted(orPlaceholder(payload.blueprintName()),
                orPlaceholder(payload.blueprintCode())),
            navigationData(event.eventType())
                .with("blueprintId", payload.blueprintId())
                .with("versionId", payload.versionId())
                .build());
    }

    private List<Draft> renderSchoolDrafts(IncomingEvent event) {
        return switch (event.eventType()) {
            case EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED -> renderDebtCapExceeded(event);
            case EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT -> renderLockedDueToDebt(event);
            case EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING -> renderQuotaUsageWarning(event);
            case EventTypeConstant.SCHOOL_DEBT_CLEARED -> renderDebtCleared(event);
            case EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED -> renderSubscriptionSuspended(event);
            case EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED -> renderSubscriptionUnsuspended(event);
            default -> throw unhandledEventType(event);
        };
    }

    private List<Draft> renderDebtCapExceeded(IncomingEvent event) {
        SchoolDebtCapExceededPayloadV1 payload = parse(event, SchoolDebtCapExceededPayloadV1.class);
        return fanOut(payload.systemAdminIds(), event.category(),
            "Warning: AI quota debt exceeds the cap",
            "%s -- debt %s exceeds the warning cap %s".formatted(quotaLabel(payload.quotaType()),
                formatAmount(payload.overageVnd()), formatAmount(payload.capVnd())),
            navigationData(event.eventType()).with("schoolId", payload.schoolId()).build());
    }

    private List<Draft> renderLockedDueToDebt(IncomingEvent event) {
        SchoolLockedDueToDebtPayloadV1 payload = parse(event, SchoolLockedDueToDebtPayloadV1.class);
        return fanOut(payload.schoolAdminIds(), event.category(),
            "The school is locked due to AI quota debt",
            "Actual AI cost has exceeded the quota -- pay, or renew or upgrade the plan, to continue organizing assessments",
            navigationData(event.eventType()).with("schoolId", payload.schoolId()).build());
    }

    private List<Draft> renderQuotaUsageWarning(IncomingEvent event) {
        SchoolQuotaUsageWarningPayloadV1 payload = parse(event, SchoolQuotaUsageWarningPayloadV1.class);
        BigDecimal percent = payload.usedAmountVnd()
            .multiply(BigDecimal.valueOf(100))
            .divide(payload.totalAllocatedVnd(), 0, RoundingMode.HALF_UP);
        return fanOut(payload.schoolAdminIds(), event.category(),
            "Warning: AI quota almost used up",
            "%s has used %s%% of its quota -- consider renewing or upgrading the plan to avoid interruption"
                .formatted(quotaLabel(payload.quotaType()), percent),
            navigationData(event.eventType()).with("schoolId", payload.schoolId()).build());
    }

    private List<Draft> renderDebtCleared(IncomingEvent event) {
        SchoolDebtClearedPayloadV1 payload = parse(event, SchoolDebtClearedPayloadV1.class);
        return fanOut(payload.schoolAdminIds(), event.category(),
            "The school has cleared its AI quota debt",
            "Quota is sufficient again -- assessments can be organized as normal",
            navigationData(event.eventType()).with("schoolId", payload.schoolId()).build());
    }

    private List<Draft> renderSubscriptionSuspended(IncomingEvent event) {
        SchoolSubscriptionSuspendedPayloadV1 payload = parse(event, SchoolSubscriptionSuspendedPayloadV1.class);
        return fanOut(payload.schoolAdminIds(), event.category(),
            "Subscription plan suspended",
            "Reason: %s".formatted(orPlaceholder(payload.reason())),
            navigationData(event.eventType()).with("schoolId", payload.schoolId()).build());
    }

    private List<Draft> renderSubscriptionUnsuspended(IncomingEvent event) {
        SchoolSubscriptionUnsuspendedPayloadV1 payload = parse(event, SchoolSubscriptionUnsuspendedPayloadV1.class);
        return fanOut(payload.schoolAdminIds(), event.category(),
            "Subscription plan unsuspended",
            "The school can use the system as normal again",
            navigationData(event.eventType()).with("schoolId", payload.schoolId()).build());
    }

    /**
     * A null recipient returns an empty list instead of a broken Draft: {@code notifications
     * .user_id} is NOT NULL, so building a Draft here would only die at the DB layer, and saveIfAbsent
     * swallows DataIntegrityViolationException so the problem would vanish without a trace.
     */
    private List<Draft> one(UUID userId, NotificationCategory category, String title, String body,
            Map<String, String> data) {
        return userId == null ? List.of() : List.of(new Draft(userId, category, title, body, data));
    }

    /**
     * The recipient list is fixed at publish time and carried in the payload, NOT queried
     * again here. That way every re-run (retry, replay from the DLT) yields exactly the same set of
     * recipients, and uk_notifications_user_event blocks duplicates cleanly. If resolved here, an admin
     * added between two retries would receive a notification about something already old -- the unique constraint
     * cannot save us because that is a different user_id.
     *
     * <p>{@code distinct} is a safety net: the payload has passed through Kafka so this side does not control
     * it, and a duplicate id costs one failed write per duplicate.
     */
    private List<Draft> fanOut(List<UUID> userIds, NotificationCategory category, String title, String body,
            Map<String, String> data) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
            .filter(userId -> userId != null)
            .distinct()
            .map(userId -> new Draft(userId, category, title, body, data))
            .toList();
    }

    /**
     * Opens a navigation map that already holds {@code eventType} and {@code target}.
     *
     * <p>{@code target} is the screen the client must open, deliberately separate from {@code eventType}:
     * five score-group events lead to the same screen, so the client only has to remember nine targets
     * instead of eighteen eventTypes. More importantly, attaching a new event to an existing target
     * is only a server-side change -- see {@link NotificationTarget}.
     */
    private NavigationData navigationData(String eventType) {
        return new NavigationData(eventType);
    }

    /**
     * Gathers the navigation keys of a notification.
     *
     * <p>Replaces the old version that accepted exactly one key: since the destination screen needs more
     * {@code sessionId}/{@code assessmentId} and {@code assessmentKind}, one key is no longer enough. A fluent form
     * instead of varargs {@code (key, value, key, value...)} because with varargs of the same type
     * nothing stops, at compile time, someone from being off by one argument.
     *
     * <p>A null or empty value is skipped instead of writing the string "null": old events still within the
     * Kafka retention do not have the new fields, and the client has to be able to tell "this key is absent" from "the key is present but
     * meaningless".
     */
    private static final class NavigationData {

        private final Map<String, String> values = new LinkedHashMap<>();

        private NavigationData(String eventType) {
            values.put("eventType", eventType);
            values.put("target", NotificationTarget.of(eventType).name());
        }

        /** FCM only accepts {@code Map<String, String>}, so every value must be stringified from here. */
        NavigationData with(String key, UUID value) {
            return with(key, value == null ? null : value.toString());
        }

        NavigationData with(String key, Enum<?> value) {
            return with(key, value == null ? null : value.name());
        }

        NavigationData with(String key, String value) {
            if (value != null && !value.isBlank()) {
                values.put(key, value);
            }
            return this;
        }

        Map<String, String> build() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }
    }

    /**
     * The push data = the payload already written to the DB, plus the id of the notification row itself.
     *
     * <p>{@code notificationId} is deliberately NOT in the payload column: a client reading the list
     * through {@code myNotifications} already has the id of each row, and storing it again would only duplicate
     * data in a column that lives forever. It is only needed on the push branch, where the client holds nothing but this map
     * when the user taps the notification tray -- without the id it does not know which row to open.
     *
     * <p>A null id means the adapter did not return the primary key after writing. Drop the key instead of
     * throwing: push is a best-effort layer, and a notification that opens the list is far better than a
     * notification that never arrives.
     */
    private Map<String, String> pushData(Pushable pushable) {
        Map<String, String> data = new LinkedHashMap<>(pushable.draft().data());

        if (pushable.notificationId() == null) {
            log.warn("Missing notificationId after the write, the push can only open the list: userId={}",
                pushable.draft().userId());
            return data;
        }

        data.put("notificationId", pushable.notificationId().toString());
        return data;
    }

    private String writePayload(Map<String, String> data) {
        return data == null || data.isEmpty() ? null : jsonMapper.writeValueAsString(data);
    }

    private <T> T parse(IncomingEvent event, Class<T> type) {
        try {
            return jsonMapper.readValue(event.value(), type);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Malformed payload: eventId=" + event.eventId() + ", eventType=" + event.eventType(), e);
        }
    }

    private void markProcessed(UUID eventId) {
        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerGroup(CONSUMER_GROUP)
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Another flow wrote first: the desired result is already achieved, nothing to do.
            log.debug("Event already marked by another flow: eventId={}", eventId);
        }
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "--" : score.stripTrailingZeros().toPlainString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "--" : AMOUNT_FORMAT.get().format(amount) + " ₫";
    }

    private String quotaLabel(QuotaType quotaType) {
        if (quotaType == null) {
            return "--";
        }
        return switch (quotaType) {
            case ASSESSMENT -> "Assessment";
            case PRACTICE -> "Personal practice session";
        };
    }

    private String formatDeadline(Instant deadline) {
        return deadline == null ? "--" : DEADLINE_FORMAT.format(deadline);
    }

    /** Same wording as GradingAssignmentEmailConsumer: the two channels talk about one grading round. */
    private String roundLabel(String roundType) {
        if (roundType == null) {
            return "Grading";
        }
        return switch (roundType) {
            case "INITIAL" -> "Initial grading";
            case "SPOT_CHECK" -> "Spot check";
            case "REMEDIATION" -> "Review of invalidated submission";
            case "APPEAL" -> "Appeal";
            default -> "Grading";
        };
    }

    private String orPlaceholder(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private record IncomingEvent(String value, String eventType, UUID eventId, NotificationCategory category) {}

    private record Draft(
        UUID userId,
        NotificationCategory category,
        String title,
        String body,
        Map<String, String> data
    ) {}

    /**
     * A {@link Draft} that was successfully written to the DB, together with the id of the row just created.
     *
     * <p>Kept apart from Draft because the id only exists after the write step: a Draft is complete from the time the content
     * is rendered, while the id is generated by the DB in {@code saveIfAbsent}. Merging it into Draft would mean a
     * field that is always null during the first half of its life, and nobody reading the code can remember which half.
     */
    private record Pushable(Draft draft, UUID notificationId) {}
}
