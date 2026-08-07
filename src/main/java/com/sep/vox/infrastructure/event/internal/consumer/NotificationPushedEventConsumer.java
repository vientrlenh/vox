package com.sep.vox.infrastructure.event.internal.consumer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.application.common.DateMapper;
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
import com.sep.vox.application.response.output.PushMessage;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationPreference;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.NotificationDeviceRepository;
import com.sep.vox.domain.repository.NotificationPreferenceRepository;
import com.sep.vox.domain.repository.NotificationRepository;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc lại đúng ba topic mà consumer mail đang dùng, bằng một consumer group riêng.
 *
 * <p>Thứ tự xử lý là phần quan trọng nhất: <b>ghi bảng notifications trước, push sau</b>.
 * Dòng trong DB mới là bản ghi bền vững -- người dùng chưa đăng ký thiết bị nào, hoặc FCM
 * đang sập, vẫn phải thấy thông báo khi mở app. Push chỉ là lớp đánh động phía trên; hỏng
 * thì bỏ qua chứ không kéo cả message vào vòng retry.
 *
 * <p>Cũng vì vậy mà consumer này parse chính payload của các event nghiệp vụ
 * ({@code ExamResultReleasedPayloadV1}...) rồi tự dựng title/body, thay vì trông chờ một
 * payload notification dựng sẵn: message trên ba topic đó do luồng mail sinh ra, chúng
 * mang hình dạng nghiệp vụ chứ không mang hình dạng thông báo.
 */
@Component
public class NotificationPushedEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationPushedEventConsumer.class);

    private static final String CONSUMER_GROUP = "notification";

    /** {@code withZone} là phần bắt buộc: thiếu nó thì thông báo ghi giờ UTC của container. */
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    private final NotificationRepository notificationRepository;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PushNotificationPort pushNotificationPort;
    private final JsonMapper jsonMapper;

    public NotificationPushedEventConsumer(
            NotificationRepository notificationRepository,
            NotificationDeviceRepository notificationDeviceRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ProcessedEventRepository processedEventRepository,
            PushNotificationPort pushNotificationPort,
            JsonMapper jsonMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.processedEventRepository = processedEventRepository;
        this.pushNotificationPort = pushNotificationPort;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        kafkaTemplate = "outboxKafkaTemplate",
        // Tên retry topic chỉ suy ra từ tên topic gốc, không từ consumer group. Thiếu hai
        // hậu tố riêng này thì mail và notification dùng chung retry topic, và một message
        // lỗi ở phía mail sẽ bị notification xử lý lại.
        retryTopicSuffix = "-notif-retry",
        dltTopicSuffix = "-notif-dlt"
    )
    @KafkaListener(
        topics = {
            "${app.internal-event.kafka.consumer-groups.notification.topic.exam-appeal}",
            "${app.internal-event.kafka.consumer-groups.notification.topic.exam-result-lifecycle}",
            "${app.internal-event.kafka.consumer-groups.notification.topic.grading-assignment}"
        },
        groupId = "${app.internal-event.kafka.consumer-groups.notification.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory",
        // Ghi đè auto-offset-reset: earliest của cấu hình chung. Đây là consumer group mới,
        // chưa có offset nào được commit, nên với earliest nó sẽ đọc lại toàn bộ lịch sử còn
        // trong retention và bắn push hàng loạt về những việc đã cũ ngay lần deploy đầu tiên.
        properties = { "auto.offset.reset=latest" }
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event notification: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        var eventType = KafkaEventHeaders.readEventType(record);

        // eventType lạ nghĩa là có event mới mà quên khai báo category. Đánh dấu đã xử lý
        // rồi bỏ qua: chạy hết vòng retry cũng không đổi được kết quả, còn để rơi vào DLT
        // thì chỉ tạo nhiễu. Lưới an toàn thật nằm ở NotificationCategoryMappingValidator,
        // chặn ngay lúc khởi động.
        if (!NotificationCategory.isMapped(eventType)) {
            LOGGER.warn("Skip notification, eventType chưa ánh xạ category: eventId={}, eventType={}",
                eventId, eventType);
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        var draft = renderDraft(eventType, eventId, record.value());

        // Bước bền vững: ghi bảng trước mọi thứ khác.
        var saved = notificationRepository.saveIfAbsent(new Notification(
            draft.userId(),
            eventId,
            eventType,
            draft.title(),
            draft.body(),
            writePayload(draft.data()),
            null,
            Instant.now()
        ));

        if (saved.isEmpty()) {
            // uk_notifications_user_event đã chặn: message từng được xử lý xong nhưng chưa
            // kịp markProcessed (crash giữa hai bước). Không push lần thứ hai.
            LOGGER.info("Notification đã tồn tại, bỏ qua push: eventId={}, userId={}",
                eventId, draft.userId());
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        pushBestEffort(draft, eventId);

        markProcessed(eventId);

        LOGGER.info("Notification created: eventId={}, eventType={}, userId={}",
            eventId, eventType, draft.userId());
        ack.acknowledge();
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        LOGGER.error("Notification event vào DLT: topic={}, partition={}, offset={}, payload={}",
            record.topic(), record.partition(), record.offset(), record.value());
    }

    /**
     * Push không bao giờ được ném lỗi ra ngoài: dòng notification đã ghi xong, và message
     * chạy lại cũng chỉ dừng ở nhánh saveIfAbsent rỗng phía trên chứ không push lại.
     */
    private void pushBestEffort(Draft draft, UUID eventId) {
        if (!pushNotificationPort.isEnabled()) {
            return;
        }

        if (!pushEnabled(draft.userId(), draft.category())) {
            LOGGER.debug("Người dùng đã tắt push nhóm {}: userId={}", draft.category(), draft.userId());
            return;
        }

        var installationIds = notificationDeviceRepository.findByUserId(draft.userId()).stream()
            .map(device -> device.getInstallationId())
            .toList();

        if (installationIds.isEmpty()) {
            LOGGER.debug("Không có thiết bị nào đăng ký: userId={}", draft.userId());
            return;
        }

        var result = pushNotificationPort.send(
            new PushMessage(draft.title(), draft.body(), draft.data()), installationIds);

        // Chỉ nhóm stale mới bị xoá. Nhóm retryable phải giữ nguyên -- xoá theo lỗi tạm thời
        // đồng nghĩa với quét sạch bảng thiết bị sau một lần FCM downtime.
        if (!result.staleInstallationIds().isEmpty()) {
            var removed = notificationDeviceRepository.deleteByInstallationIdIn(result.staleInstallationIds());
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

    private Draft renderDraft(String eventType, UUID eventId, String value) {
        var category = NotificationCategory.of(eventType);

        return switch (eventType) {
            case EventTypeConstant.EXAM_RESULT_RELEASED -> {
                var payload = parse(value, ExamResultReleasedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Điểm thi của bạn đã có",
                    "%s: %s điểm".formatted(orPlaceholder(payload.examName()), formatScore(payload.totalScore())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_REGRADED -> {
                var payload = parse(value, ExamResultRegradedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Điểm thi của bạn đã thay đổi",
                    "%s: %s -> %s điểm".formatted(orPlaceholder(payload.examName()),
                        formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_INVALIDATED -> {
                var payload = parse(value, ExamResultInvalidatedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Bài thi của bạn đã bị vô hiệu",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_INVALID_CLEARED -> {
                var payload = parse(value, ExamResultInvalidClearedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Bài thi của bạn đã được khôi phục",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED -> {
                var payload = parse(value, ExamResultOutcomeDecidedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Kết quả cuối cùng của bạn",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.outcome())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }

            case EventTypeConstant.EXAM_APPEAL_PUBLISHED -> {
                var payload = parse(value, ExamAppealPublishedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Kết quả phúc khảo đã được công bố",
                    "%s: %s -> %s điểm".formatted(orPlaceholder(payload.examName()),
                        formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
                    data(eventType, "appealId", payload.appealId()));
            }
            case EventTypeConstant.EXAM_APPEAL_REJECTED -> {
                var payload = parse(value, ExamAppealRejectedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Đơn phúc khảo không được chấp nhận",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "appealId", payload.appealId()));
            }
            case EventTypeConstant.EXAM_APPEAL_APPROVED -> {
                var payload = parse(value, ExamAppealApprovedPayloadV1.class, eventType, eventId);
                yield new Draft(payload.studentId(), category,
                    "Đơn phúc khảo của bạn đã được duyệt",
                    "%s -- dự kiến có kết quả trước %s".formatted(
                        orPlaceholder(payload.examName()), formatDeadline(payload.deadline())),
                    data(eventType, "appealId", payload.appealId()));
            }

            case EventTypeConstant.GRADING_DEADLINE_REMINDER -> {
                var payload = parse(value, GradingDeadlineReminderPayloadV1.class, eventType, eventId);
                yield new Draft(payload.teacherId(), category,
                    "Sắp tới hạn chấm bài",
                    "%s -- hạn %s".formatted(orPlaceholder(payload.examName()),
                        formatDeadline(payload.deadlineAt())),
                    data(eventType, "assignmentId", payload.assignmentId()));
            }
            case EventTypeConstant.GRADING_ASSIGNMENT_DECLINED -> {
                var payload = parse(value, GradingAssignmentDeclinedPayloadV1.class, eventType, eventId);
                // Người nhận là admin đã giao việc, không phải giáo viên trả lại việc.
                yield new Draft(payload.assignedBy(), category,
                    "Có người trả lại phân công chấm",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "assignmentId", payload.assignmentId()));
            }

            default -> throw new IllegalStateException(
                "eventType không được xử lý: eventId=" + eventId + ", eventType=" + eventType);
        };
    }

    /** FCM chỉ nhận {@code Map<String, String>}, nên mọi giá trị phải stringify từ đây. */
    private Map<String, String> data(String eventType, String idKey, UUID idValue) {
        var data = new LinkedHashMap<String, String>();
        data.put("eventType", eventType);
        if (idValue != null) {
            data.put(idKey, idValue.toString());
        }
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
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            // Luồng khác đã ghi trước: kết quả mong muốn đã đạt được, không có gì phải làm.
            LOGGER.debug("Event đã được đánh dấu bởi luồng khác: eventId={}", eventId);
        }
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "--" : score.stripTrailingZeros().toPlainString();
    }

    private String formatDeadline(Instant deadline) {
        return deadline == null ? "--" : DEADLINE_FORMAT.format(deadline);
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
}
