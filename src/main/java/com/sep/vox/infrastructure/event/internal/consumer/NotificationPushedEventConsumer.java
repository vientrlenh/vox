package com.sep.vox.infrastructure.event.internal.consumer;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.sep.vox.application.event.ExamAppealApprovedPayloadV1;
import com.sep.vox.application.event.ExamAppealPublishedPayloadV1;
import com.sep.vox.application.event.ExamAppealRejectedPayloadV1;
import com.sep.vox.application.event.ExamBlueprintVersionPublishedEvent;
import com.sep.vox.application.event.ExamResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.ExamResultInvalidatedPayloadV1;
import com.sep.vox.application.event.ExamResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.event.ExamResultRegradedPayloadV1;
import com.sep.vox.application.event.ExamResultReleasedPayloadV1;
import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.event.InvoicePaidPayloadV1;
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
 * Đọc ba topic mà consumer mail cũng đang dùng (bằng một consumer group riêng), cộng thêm
 * topic exam-blueprint-version vốn chỉ có notification đọc.
 *
 * <p>Thứ tự xử lý là phần quan trọng nhất: <b>ghi bảng notifications trước, push sau</b>.
 * Dòng trong DB mới là bản ghi bền vững -- người dùng chưa đăng ký thiết bị nào, hoặc FCM
 * đang sập, vẫn phải thấy thông báo khi mở app. Push chỉ là lớp đánh động phía trên; hỏng
 * thì bỏ qua chứ không kéo cả message vào vòng retry.
 *
 * <p>Cũng vì vậy mà consumer này parse chính payload của các event nghiệp vụ
 * ({@code ExamResultReleasedPayloadV1}...) rồi tự dựng title/body, thay vì trông chờ một
 * payload notification dựng sẵn: message trên các topic đó mang hình dạng nghiệp vụ chứ
 * không mang hình dạng thông báo.
 *
 * <p>Một event có thể sinh nhiều dòng notification. Danh sách người nhận luôn nằm sẵn
 * trong payload, không truy vấn lại ở đây -- xem {@link #fanOut}.
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
            "${app.internal-event.kafka.consumer-groups.notification.topic.grading-assignment}", 
            "${app.internal-event.kafka.consumer-groups.notification.topic.exam-blueprint-version}"
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

        var drafts = renderDrafts(eventType, eventId, record.value());

        if (drafts.isEmpty()) {
            // Payload không chỉ ra người nhận nào. Với event fan-out là trường không còn
            // admin nào đang hoạt động; với event một người nhận là payload thiếu id. Cả hai
            // đều không sửa được bằng cách chạy lại.
            LOGGER.warn("Skip notification, không có người nhận: eventId={}, eventType={}",
                eventId, eventType);
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        // Bước bền vững: ghi bảng trước mọi thứ khác. Cùng một Instant cho cả lô để mọi
        // người nhận của một event có createdAt giống nhau -- cursor phân trang sắp theo id
        // nên không bị ảnh hưởng, nhưng lệch vài chục ms giữa các dòng cùng event chỉ gây
        // khó hiểu khi soi log.
        var now = Instant.now();
        var pushable = new ArrayList<Draft>();
        for (var draft : drafts) {
            var saved = notificationRepository.saveIfAbsent(new Notification(
                draft.userId(),
                eventId,
                eventType,
                draft.title(),
                draft.body(),
                writePayload(draft.data()),
                null,
                now
            ));

            // Quyết định push phải là quyết định của TỪNG người nhận, không phải của cả
            // message: crash giữa chừng ở lần trước có thể đã tạo xong 30/50 dòng, và lần
            // này chỉ 20 người còn lại mới xứng đáng nhận push.
            if (saved.isPresent()) {
                pushable.add(draft);
            }
        }

        if (pushable.isEmpty()) {
            // uk_notifications_user_event đã chặn toàn bộ: message từng được xử lý xong nhưng
            // chưa kịp markProcessed (crash giữa hai bước). Không push lần thứ hai.
            LOGGER.info("Notification đã tồn tại cho cả {} người nhận, bỏ qua push: eventId={}",
                drafts.size(), eventId);
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        // Nuốt lỗi ở cả hai chỗ dưới đây là CHỦ Ý, khác hẳn ca SmtpEmailSendingService: dòng
        // notifications đã ghi xong ở trên, nên mất một lần push chỉ mất lớp đánh động, người
        // dùng mở app vẫn thấy đủ. Để lỗi lan ra thì message chạy lại cả vòng retry cho một
        // công việc thực ra đã hoàn thành.
        try {
            // Cả lô đi trong MỘT task, không phải mỗi người nhận một task: pushExecutor dùng
            // AbortPolicy, nên N task cho một event fan-out sẽ làm đầy hàng đợi và đánh rơi
            // push của những event khác đang chờ.
            executor.execute(() -> {
                for (var draft : pushable) {
                    try {
                        pushBestEffort(draft, eventId);
                    } catch (Exception e) {
                        // Chạy trên luồng pool: không log ở đây thì exception rơi vào uncaught
                        // handler của thread và không bao giờ vào file log. Bắt trong vòng lặp
                        // để một người nhận hỏng không cắt mất push của những người còn lại.
                        LOGGER.error("Push failed outside consumer thread: eventId={}, userId={}",
                            eventId, draft.userId(), e);
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

    /**
     * @return một Draft cho mỗi người nhận. Danh sách rỗng nghĩa là event không có người
     *         nhận nào -- phía gọi bỏ qua message thay vì đẩy vào retry.
     */
    private List<Draft> renderDrafts(String eventType, UUID eventId, String value) {
        var category = NotificationCategory.of(eventType);

        return switch (eventType) {
            case EventTypeConstant.EXAM_RESULT_RELEASED -> {
                var payload = parse(value, ExamResultReleasedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Điểm thi của bạn đã có",
                    "%s: %s điểm".formatted(orPlaceholder(payload.examName()), formatScore(payload.totalScore())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_REGRADED -> {
                var payload = parse(value, ExamResultRegradedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Điểm thi của bạn đã thay đổi",
                    "%s: %s -> %s điểm".formatted(orPlaceholder(payload.examName()),
                        formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_INVALIDATED -> {
                var payload = parse(value, ExamResultInvalidatedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Bài thi của bạn đã bị vô hiệu",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_INVALID_CLEARED -> {
                var payload = parse(value, ExamResultInvalidClearedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Bài thi của bạn đã được khôi phục",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }
            case EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED -> {
                var payload = parse(value, ExamResultOutcomeDecidedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Kết quả cuối cùng của bạn",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.outcome())),
                    data(eventType, "candidateResultId", payload.candidateResultId()));
            }

            case EventTypeConstant.EXAM_APPEAL_PUBLISHED -> {
                var payload = parse(value, ExamAppealPublishedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Kết quả phúc khảo đã được công bố",
                    "%s: %s -> %s điểm".formatted(orPlaceholder(payload.examName()),
                        formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())),
                    data(eventType, "appealId", payload.appealId()));
            }
            case EventTypeConstant.EXAM_APPEAL_REJECTED -> {
                var payload = parse(value, ExamAppealRejectedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Đơn phúc khảo không được chấp nhận",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "appealId", payload.appealId()));
            }
            case EventTypeConstant.EXAM_APPEAL_APPROVED -> {
                var payload = parse(value, ExamAppealApprovedPayloadV1.class, eventType, eventId);
                yield one(payload.studentId(), category,
                    "Đơn phúc khảo của bạn đã được duyệt",
                    "%s -- dự kiến có kết quả trước %s".formatted(
                        orPlaceholder(payload.examName()), formatDeadline(payload.deadline())),
                    data(eventType, "appealId", payload.appealId()));
            }

            case EventTypeConstant.GRADING_DEADLINE_REMINDER -> {
                var payload = parse(value, GradingDeadlineReminderPayloadV1.class, eventType, eventId);
                yield one(payload.teacherId(), category,
                    "Sắp tới hạn chấm bài",
                    "%s -- hạn %s".formatted(orPlaceholder(payload.examName()),
                        formatDeadline(payload.deadlineAt())),
                    data(eventType, "assignmentId", payload.assignmentId()));
            }
            case EventTypeConstant.GRADING_ASSIGNMENT_DECLINED -> {
                var payload = parse(value, GradingAssignmentDeclinedPayloadV1.class, eventType, eventId);
                // Người nhận là admin đã giao việc, không phải giáo viên trả lại việc.
                yield one(payload.assignedBy(), category,
                    "Có người trả lại phân công chấm",
                    "%s: %s".formatted(orPlaceholder(payload.examName()), orPlaceholder(payload.reason())),
                    data(eventType, "assignmentId", payload.assignmentId()));
            }

            case EventTypeConstant.INVOICE_PAID -> {
                var payload = parse(value, InvoicePaidPayloadV1.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Hóa đơn đã được thanh toán",
                    "Hóa đơn #%s -- %s".formatted(orPlaceholder(payload.invoiceNumber()),
                        formatAmount(payload.amount())),
                    data(eventType, "invoiceNumber", payload.invoiceNumber()));
            }

            // Cùng dạng fan-out với InvoicePaid: một blueprint được publish thì MỌI school
            // admin của trường đều cần biết.
            case EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED -> {
                var payload = parse(value, ExamBlueprintVersionPublishedEvent.class, eventType, eventId);
                yield fanOut(payload.schoolAdminIds(), category,
                    "Blueprint đề thi đã sẵn sàng",
                    "%s (%s)".formatted(orPlaceholder(payload.blueprintName()),
                        orPlaceholder(payload.blueprintCode())),
                    data(eventType, "blueprintCode", payload.blueprintCode()));
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

    /** FCM chỉ nhận {@code Map<String, String>}, nên mọi giá trị phải stringify từ đây. */
    private Map<String, String> data(String eventType, String idKey, UUID idValue) {
        return data(eventType, idKey, idValue == null ? null : idValue.toString());
    }

    /** Biến thể cho khoá điều hướng vốn đã là chuỗi (mã blueprint...), không phải UUID. */
    private Map<String, String> data(String eventType, String key, String value) {
        var data = new LinkedHashMap<String, String>();
        data.put("eventType", eventType);
        if (value != null && !value.isBlank()) {
            data.put(key, value);
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

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "--" : AMOUNT_FORMAT.get().format(amount) + " ₫";
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
