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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class NotificationPushedEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationPushedEventConsumer.class);

    private static final String CONSUMER_GROUP = "notification";

    /** {@code withZone} là phần bắt buộc: thiếu nó thì thông báo ghi giờ UTC của container. */
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    /** DecimalFormat KHÔNG thread-safe, nên đây phải là ThreadLocal chứ không phải field dùng chung. */
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

        LOGGER.info("Receive event notification: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        String eventType = KafkaEventHeaders.readEventType(record);

        // An unknown eventType means a new event was added without declaring its category. Mark it processed
        // and skip: running the whole retry cycle cannot change the outcome, and letting it fall into the DLT
        // only creates noise. The real safety net is NotificationCategoryMappingValidator,
        // which blocks at startup.
        if (!NotificationCategory.isMapped(eventType)) {
            LOGGER.warn("Skip notification, eventType chưa ánh xạ category: eventId={}, eventType={}",
                eventId, eventType);
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        List<Draft> drafts = renderDrafts(eventType, eventId, record.value());

        if (drafts.isEmpty()) {
            // The payload names no recipient. For a fan-out event that means no active
            // admin is left; for a single-recipient event the payload is missing an id. Neither
            // can be fixed by running it again.
            LOGGER.warn("Skip notification, không có người nhận: eventId={}, eventType={}",
                eventId, eventType);
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        // Durable step: write the table before anything else. One Instant for the whole batch so that every
        // recipient of an event has the same createdAt -- the pagination cursor sorts by id
        // so it is not affected, but a difference of a few dozen ms between rows of the same event only causes
        // confusion when reading logs.
        Instant now = Instant.now();
        List<Pushable> pushable = new ArrayList<Pushable>();
        for (Draft draft : drafts) {
            Optional<Notification> saved = notificationRepository.saveIfAbsent(Notification.builder()
                    .userId(draft.userId())
                    .eventId(eventId)
                    .eventType(eventType)
                    .title(draft.title())
                    .body(draft.body())
                    .payload(writePayload(draft.data()))
                    .readAt(null)
                    .createdAt(now)
                    .build());

            // The decision to push must be made PER RECIPIENT, not for the whole
            // message: a crash midway last time may have already created 30 of 50 rows, and this
            // time only the remaining 20 people deserve a push.
            if (saved.isPresent()) {
                pushable.add(new Pushable(draft, saved.get().getId()));
            }
        }

        if (pushable.isEmpty()) {
            // uk_notifications_user_event blocked everything: the message was fully processed before but
            // markProcessed had not run yet (crash between the two steps). Do not push a second time.
            LOGGER.info("Notification đã tồn tại cho cả {} người nhận, bỏ qua push: eventId={}",
                drafts.size(), eventId);
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        // Swallowing errors in both places below is INTENTIONAL, unlike the SmtpEmailSendingService case: the
        // notifications row has already been written above, so losing one push only loses the alert layer, the
        // user still sees everything when opening the app. Letting the error propagate would rerun the whole retry cycle for
        // work that is actually already done.
        try {
            // The whole batch goes in ONE task, not one task per recipient: pushExecutor uses
            // AbortPolicy, so N tasks for one fan-out event would fill the queue and drop the
            // pushes of other events that are waiting.
            executor.execute(() -> {
                for (Pushable entry : pushable) {
                    try {
                        pushBestEffort(entry, eventId);
                    } catch (Exception e) {
                        // Runs on a pool thread: without logging here the exception falls into the thread's uncaught
                        // handler and never reaches the log file. Caught inside the loop so that one failing
                        // recipient does not cut off the pushes of the remaining ones.
                        LOGGER.error("Push failed outside consumer thread: eventId={}, userId={}",
                            eventId, entry.draft().userId(), e);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.warn("Push queue is full, skipping push: eventId={}, recipients={}",
                eventId, pushable.size());
        }

        markProcessed(eventId);

        LOGGER.info("Notification created: eventId={}, eventType={}, recipients={}, pushable={}",
            eventId, eventType, drafts.size(), pushable.size());
        ack.acknowledge();
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        LOGGER.error("Notification event vào DLT: topic={}, partition={}, offset={}, payload={}",
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
            LOGGER.debug("Người dùng đã tắt push nhóm {}: userId={}", draft.category(), draft.userId());
            return;
        }

        List<String> installationIds = notificationDeviceRepository.findByUserId(draft.userId()).stream()
            .map(device -> device.getInstallationId())
            .toList();

        if (installationIds.isEmpty()) {
            LOGGER.debug("Không có thiết bị nào đăng ký: userId={}", draft.userId());
            return;
        }

        PushDispatchResult result = pushNotificationPort.send(
            new PushMessage(draft.title(), draft.body(), pushData(pushable)), installationIds);

        // Only the stale group is deleted. The retryable group must be kept -- deleting on a transient error
        // would mean wiping the whole device table after a single FCM downtime.
        if (!result.staleInstallationIds().isEmpty()) {
            int removed = notificationDeviceRepository.deleteByInstallationIdIn(result.staleInstallationIds());
            LOGGER.info("Dọn {} thiết bị không còn dùng được: eventId={}", removed, eventId);
        }

        LOGGER.debug("Push xong: eventId={}, success={}, stale={}, retryable={}",
            eventId, result.successCount(),
            result.staleInstallationIds().size(), result.retryableInstallationIds().size());
    }

    private boolean pushEnabled(UUID userId, NotificationCategory category) {
        return notificationPreferenceRepository.findByUserIdAndCategory(userId, category)
            .map(preference -> preference.isPushEnabled())
            // Không có dòng nghĩa là chưa từng đổi thiết lập -> dùng mặc định.
            .orElse(NotificationPreference.DEFAULT_PUSH_ENABLED);
    }

    /**
     * @return one Draft per recipient. An empty list means the event has no
     *         recipient at all -- the caller skips the message instead of pushing it into retry.
     */
    private List<Draft> renderDrafts(String eventType, UUID eventId, String value) {
        NotificationCategory category = NotificationCategory.of(eventType);

        return switch (eventType) {
            case EventTypeConstant.ASSESSMENT_RESULT_RELEASED -> {
                AssessmentResultReleasedPayloadV1 payload = parse(value, AssessmentResultReleasedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Điểm thi của bạn đã có",
                    "%s: %s điểm".formatted(orPlaceholder(payload.assessmentName()), formatScore(payload.totalScore())),
                    data(eventType)
                        .with("candidateResultId", payload.candidateResultId())
                        .with("sessionId", payload.sessionId())
                        .with("assessmentKind", payload.assessmentKind())
                        .with("assessmentName", payload.assessmentName())
                        .build());
            }
            case EventTypeConstant.ASSESSMENT_RESULT_REGRADED -> {
                AssessmentResultRegradedPayloadV1 payload = parse(value, AssessmentResultRegradedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Điểm thi của bạn đã thay đổi",
                    "%s: %s -> %s điểm".formatted(orPlaceholder(payload.assessmentName()),
                        formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
                    data(eventType)
                        .with("candidateResultId", payload.candidateResultId())
                        .with("sessionId", payload.sessionId())
                        .with("assessmentKind", payload.assessmentKind())
                        .with("assessmentName", payload.assessmentName())
                        .build());
            }
            case EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED -> {
                AssessmentResultInvalidatedPayloadV1 payload = parse(value, AssessmentResultInvalidatedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Bài thi của bạn đã bị vô hiệu",
                    "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
                    data(eventType)
                        .with("candidateResultId", payload.candidateResultId())
                        .with("sessionId", payload.sessionId())
                        .with("assessmentKind", payload.assessmentKind())
                        .with("assessmentName", payload.assessmentName())
                        .build());
            }
            case EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED -> {
                AssessmentResultInvalidClearedPayloadV1 payload = parse(value, AssessmentResultInvalidClearedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Bài thi của bạn đã được khôi phục",
                    "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
                    data(eventType)
                        .with("candidateResultId", payload.candidateResultId())
                        .with("sessionId", payload.sessionId())
                        .with("assessmentKind", payload.assessmentKind())
                        .with("assessmentName", payload.assessmentName())
                        .build());
            }
            case EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED -> {
                AssessmentResultOutcomeDecidedPayloadV1 payload = parse(value, AssessmentResultOutcomeDecidedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Kết quả cuối cùng của bạn",
                    "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.outcome())),
                    data(eventType)
                        .with("candidateResultId", payload.candidateResultId())
                        .with("sessionId", payload.sessionId())
                        .with("assessmentKind", payload.assessmentKind())
                        .with("assessmentName", payload.assessmentName())
                        .build());
            }

            case EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED -> {
                AssessmentAppealPublishedPayloadV1 payload = parse(value, AssessmentAppealPublishedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Kết quả phúc khảo đã được công bố",
                    "%s: %s -> %s điểm".formatted(orPlaceholder(payload.assessmentName()),
                        formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
                    data(eventType).with("appealId", payload.appealId()).build());
            }
            case EventTypeConstant.ASSESSMENT_APPEAL_REJECTED -> {
                AssessmentAppealRejectedPayloadV1 payload = parse(value, AssessmentAppealRejectedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Đơn phúc khảo không được chấp nhận",
                    "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
                    data(eventType).with("appealId", payload.appealId()).build());
            }
            case EventTypeConstant.ASSESSMENT_APPEAL_APPROVED -> {
                AssessmentAppealApprovedPayloadV1 payload = parse(value, AssessmentAppealApprovedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Đơn phúc khảo của bạn đã được duyệt",
                    "%s -- dự kiến có kết quả trước %s".formatted(
                        orPlaceholder(payload.assessmentName()), formatDeadline(payload.deadline())),
                    data(eventType).with("appealId", payload.appealId()).build());
            }

            case EventTypeConstant.GRADING_DEADLINE_REMINDER -> {
                GradingDeadlineReminderPayloadV1 payload = parse(value, GradingDeadlineReminderPayloadV1.class, eventType, eventId);
                yield one(payload.teacherId(), category,
                    "Sắp tới hạn chấm bài",
                    "%s -- hạn %s".formatted(orPlaceholder(payload.assessmentName()),
                        formatDeadline(payload.deadlineAt())),
                    data(eventType)
                        .with("assignmentId", payload.assignmentId())
                        .with("assessmentId", payload.assessmentId())
                        .with("assessmentKind", payload.assessmentKind())
                        .build());
            }
            case EventTypeConstant.GRADING_ASSIGNMENT_DECLINED -> {
                GradingAssignmentDeclinedPayloadV1 payload = parse(value, GradingAssignmentDeclinedPayloadV1.class, eventType, eventId);
                // The recipient is the admin who assigned the work, not the teacher who returned it.
                yield one(payload.assignedBy(), category,
                    "Có người trả lại phân công chấm",
                    "%s: %s".formatted(orPlaceholder(payload.assessmentName()), orPlaceholder(payload.reason())),
                    data(eventType)
                        .with("assignmentId", payload.assignmentId())
                        .with("assessmentId", payload.assessmentId())
                        .with("assessmentKind", payload.assessmentKind())
                        .build());
            }

            case EventTypeConstant.GRADING_ASSIGNMENT_OPENED -> {
                GradingAssignmentOpenedPayloadV1 payload = parse(value, GradingAssignmentOpenedPayloadV1.class, eventType, eventId);
                yield one(payload.teacherId(), category,
                    "Bạn được giao chấm bài",
                    // candidateLabel was already chosen by the anonymity rule at publish time: the student's name
                    // for a class test, the attempt code for a centralized assessment. Do NOT re-derive it here.
                    "%s - %s: %s".formatted(
                        orPlaceholder(payload.assessmentName()),
                        roundLabel(payload.roundType()),
                        orPlaceholder(payload.candidateLabel())),
                    data(eventType)
                        .with("assignmentId", payload.assignmentId())
                        .with("assessmentId", payload.assessmentId())
                        .with("assessmentKind", payload.assessmentKind())
                        .build());
            }

            case EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED -> {
                AssessmentHumanGradingRequiredPayloadV1 payload = parse(value, AssessmentHumanGradingRequiredPayloadV1.class, eventType, eventId);
                yield fanOut(payload.recipientIds(), category,
                    "Có bài chờ soát điểm AI",
                    "%s: %d bài cần chấm tay".formatted(
                        orPlaceholder(payload.assessmentName()), payload.pendingCount()),
                    data(eventType)
                        .with("assessmentId", payload.assessmentId())
                        .with("assessmentKind", payload.assessmentKind())
                        .build());
            }

            case EventTypeConstant.INVOICE_PAID -> {
                InvoicePaidPayloadV1 payload = parse(value, InvoicePaidPayloadV1.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Hóa đơn đã được thanh toán",
                    "Hóa đơn #%s -- %s".formatted(orPlaceholder(payload.invoiceNumber()),
                        formatAmount(payload.amount())),
                    data(eventType).with("invoiceNumber", payload.invoiceNumber()).build());
            }

            // Same fan-out shape as InvoicePaid: when a blueprint is published, EVERY school
            // admin of the school needs to know.
            case EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED -> {
                AssessmentBlueprintVersionPublishedEvent payload = parse(value, AssessmentBlueprintVersionPublishedEvent.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Blueprint đề thi đã sẵn sàng",
                    "%s (%s)".formatted(orPlaceholder(payload.blueprintName()),
                        orPlaceholder(payload.blueprintCode())),
                    data(eventType)
                        .with("blueprintId", payload.blueprintId())
                        .with("versionId", payload.versionId())
                        .build());
            }

            case EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED -> {
                SchoolDebtCapExceededPayloadV1 payload = parse(value, SchoolDebtCapExceededPayloadV1.class, eventType, eventId);
                yield fanOut(payload.systemAdminIds(), category,
                    "Cảnh báo: nợ hạn mức AI vượt trần",
                    "%s -- nợ %s vượt trần cảnh báo %s".formatted(quotaLabel(payload.quotaType()),
                        formatAmount(payload.overageVnd()), formatAmount(payload.capVnd())),
                    data(eventType).with("schoolId", payload.schoolId()).build());
            }

            case EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT -> {
                SchoolLockedDueToDebtPayloadV1 payload = parse(value, SchoolLockedDueToDebtPayloadV1.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Trường đang bị khóa do nợ hạn mức AI",
                    "Chi phí AI thực tế đã vượt hạn mức -- thanh toán hoặc gia hạn/nâng cấp gói để tiếp tục tổ chức thi",
                    data(eventType).with("schoolId", payload.schoolId()).build());
            }

            case EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING -> {
                SchoolQuotaUsageWarningPayloadV1 payload = parse(value, SchoolQuotaUsageWarningPayloadV1.class, eventType, eventId);
                BigDecimal percent = payload.usedAmountVnd()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(payload.totalAllocatedVnd(), 0, RoundingMode.HALF_UP);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Cảnh báo: sắp hết hạn mức AI",
                    "%s đã dùng %s%% hạn mức -- cân nhắc gia hạn/nâng cấp gói để tránh gián đoạn"
                        .formatted(quotaLabel(payload.quotaType()), percent),
                    data(eventType).with("schoolId", payload.schoolId()).build());
            }

            case EventTypeConstant.SCHOOL_DEBT_CLEARED -> {
                SchoolDebtClearedPayloadV1 payload = parse(value, SchoolDebtClearedPayloadV1.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Trường đã hết nợ hạn mức AI",
                    "Đã đủ hạn mức trở lại -- có thể tổ chức thi bình thường",
                    data(eventType).with("schoolId", payload.schoolId()).build());
            }

            case EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED -> {
                SchoolSubscriptionSuspendedPayloadV1 payload = parse(value, SchoolSubscriptionSuspendedPayloadV1.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Gói subscription bị đình chỉ",
                    "Lý do: %s".formatted(orPlaceholder(payload.reason())),
                    data(eventType).with("schoolId", payload.schoolId()).build());
            }

            case EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED -> {
                SchoolSubscriptionUnsuspendedPayloadV1 payload = parse(value, SchoolSubscriptionUnsuspendedPayloadV1.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Gói subscription đã được gỡ đình chỉ",
                    "Trường có thể sử dụng bình thường trở lại",
                    data(eventType).with("schoolId", payload.schoolId()).build());
            }

            default -> throw new IllegalStateException(
                "eventType không được xử lý: eventId=" + eventId + ", eventType=" + eventType);
        };
    }

    /**
     * Người nhận null trả về danh sách rỗng thay vì một Draft hỏng: {@code notifications
     * .user_id} là NOT NULL, nên dựng Draft ở đây chỉ để chết ở tầng DB, và saveIfAbsent
     * nuốt DataIntegrityViolationException nên sự cố sẽ biến mất không dấu vết.
     */
    private List<Draft> one(UUID userId, NotificationCategory category, String title, String body,
            Map<String, String> data) {
        return userId == null ? List.of() : List.of(new Draft(userId, category, title, body, data));
    }

    /**
     * Danh sách người nhận được chốt từ lúc publish và đi kèm trong payload, KHÔNG truy vấn
     * lại ở đây. Nhờ vậy mỗi lần chạy lại (retry, replay từ DLT) đều cho ra đúng tập người
     * nhận cũ, và uk_notifications_user_event chặn được sạch. Nếu resolve tại đây, một admin
     * mới được thêm giữa hai lần retry sẽ nhận thông báo về việc đã cũ -- ràng buộc unique
     * không cứu được vì đó là user_id khác.
     *
     * <p>{@code distinct} là lưới an toàn: payload đã đi qua Kafka nên phía này không kiểm
     * soát được nó, mà id trùng sẽ tốn một lượt ghi hỏng cho mỗi bản trùng.
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
     * Mở một map điều hướng đã có sẵn {@code eventType} và {@code target}.
     *
     * <p>{@code target} là màn hình client cần mở, tách khỏi {@code eventType} có chủ ý:
     * năm event nhóm điểm cùng dẫn về một màn hình, nên client chỉ phải nhớ chín target
     * thay vì mười tám eventType. Quan trọng hơn, gắn thêm một event mới vào một target đã
     * có chỉ là thay đổi phía server -- xem {@link NotificationTarget}.
     */
    private NavigationData data(String eventType) {
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

        /** FCM chỉ nhận {@code Map<String, String>}, nên mọi giá trị phải stringify từ đây. */
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
            LOGGER.warn("Thiếu notificationId sau khi ghi, push sẽ chỉ mở được danh sách: userId={}",
                pushable.draft().userId());
            return data;
        }

        data.put("notificationId", pushable.notificationId().toString());
        return data;
    }

    private String writePayload(Map<String, String> data) {
        return data == null || data.isEmpty() ? null : jsonMapper.writeValueAsString(data);
    }

    private <T> T parse(String value, Class<T> type, String eventType, UUID eventId) {
        try {
            return jsonMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Payload sai định dạng: eventId=" + eventId + ", eventType=" + eventType, e);
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
            // Luồng khác đã ghi trước: kết quả mong muốn đã đạt được, không có gì phải làm.
            LOGGER.debug("Event đã được đánh dấu bởi luồng khác: eventId={}", eventId);
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
            case ASSESSMENT -> "Bài kiểm tra";
            case PRACTICE -> "Lượt ôn luyện cá nhân";
        };
    }

    private String formatDeadline(Instant deadline) {
        return deadline == null ? "--" : DEADLINE_FORMAT.format(deadline);
    }

    /** Cùng bộ chữ với GradingAssignmentEmailConsumer: hai kênh nói về một vòng chấm. */
    private String roundLabel(String roundType) {
        if (roundType == null) {
            return "Chấm bài";
        }
        return switch (roundType) {
            case "INITIAL" -> "Chấm lần đầu";
            case "SPOT_CHECK" -> "Hậu kiểm";
            case "REMEDIATION" -> "Xem xét bài bị vô hiệu";
            case "APPEAL" -> "Phúc khảo";
            default -> "Chấm bài";
        };
    }

    private String orPlaceholder(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private record Draft(
        UUID userId,
        NotificationCategory category,
        String title,
        String body,
        Map<String, String> data
    ) {}

    /**
     * Một {@link Draft} đã ghi xuống DB thành công, kèm id của dòng vừa tạo.
     *
     * <p>Tách khỏi Draft vì id chỉ tồn tại sau bước ghi: Draft dựng xong từ lúc render nội
     * dung, còn id do DB sinh ra trong {@code saveIfAbsent}. Gộp vào Draft nghĩa là có một
     * ô luôn null trong suốt nửa đầu vòng đời, và không ai đọc code nhớ được nửa nào.
     */
    private record Pushable(Draft draft, UUID notificationId) {}
}
