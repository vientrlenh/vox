package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.infrastructure.properties.PracticeGenerationProperties;
import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionClaimService;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionPersistenceService;
import com.sep.vox.domain.model.personalization.PracticeQuestion;

/**
 * Resolve ĐÚNG 1 câu MAIN tiếp theo trong lúc phiên luyện đang chạy -- gọi từ Python (nội bộ,
 * PracticeInternalSecretFilter bảo vệ) khi decision.should_continue == False. Xem gói 11 mục
 * 2.4 bước 4.
 *
 * KHÔNG @Transactional ở tầng này: selectionService.resolveNextQuestion (gọi trong doExecute)
 * có thể gọi LLM sinh câu hỏi trực tiếp (bậc 4, 10-20s) khi topic còn thưa câu -- giữ 1
 * transaction/connection DB mở suốt lúc đó từng gây HikariCP cạn pool dưới tải thật (xem
 * BuildPracticePaperUseCase, cùng lớp bug, sửa cùng đợt).
 *
 * Việc serialize các lời gọi trùng cho CÙNG 1 session (Python's timeout-retry-8s racing lần gọi
 * gốc vẫn đang chạy, xem practice_session_client.py) trước đây dựa vào SELECT ... FOR UPDATE giữ
 * suốt cả method -- giờ thay bằng khoá JVM keyed theo sessionId, giữ đúng phạm vi tương đương
 * (đủ vì Python luôn retry vào lại đúng instance vox-app này) nhưng không chiếm connection DB khi
 * đang chờ mạng. Transaction thật (FOR UPDATE + đọc, rồi ghi) được tách thành 2 bean riêng
 * (ResolveNextPracticeQuestionClaimService / ...PersistenceService) bao quanh đúng phần DB, với
 * cuộc gọi LLM chậm nằm GIỮA, ngoài mọi transaction.
 */
@Service
public class ResolveNextPracticeQuestionUseCase {

    public record QuestionPayload(
        UUID questionId,
        int slot,
        String questionText,
        String criterionCode,
        String subAttribute,
        int difficultyRank,
        int maxResponseSeconds,
        int minResponseSeconds,
        List<String> suggestedIdeas,
        /**
         * Câu CUỐI của phiên: ngân sách còn lại không đủ cho một câu cỡ bình thường, nên
         * câu này đã được may đo vừa đúng phần còn lại. Trả lời xong là đóng phiên, không
         * hỏi câu tiếp -- xem cách tính ở {@link #toPayload}.
         */
        boolean lastQuestion) {
    }

    /**
     * {@code sessionSpokenSeconds}/{@code sessionBudgetSeconds}: cặp số cho thanh tiến độ trên
     * máy học sinh. Đi kèm câu hỏi vì đây là thứ Python gửi xuống client NGAY khi phiên mở --
     * lượt đầu chưa nộp nên chưa có SubmitTurnResult nào để lấy ngân sách từ đó.
     */
    public record Result(
        String status,
        String reason,
        QuestionPayload question,
        int sessionSpokenSeconds,
        int sessionBudgetSeconds) {

        static Result ok(QuestionPayload question, int spokenSeconds, int budgetSeconds) {
            return new Result("ok", null, question, spokenSeconds, budgetSeconds);
        }

        static Result noMoreQuestions(String reason, int spokenSeconds, int budgetSeconds) {
            return new Result("no_more_questions", reason, null, spokenSeconds, budgetSeconds);
        }
    }

    // Không dọn dẹp entry cũ: ở quy mô hiện tại (demo, vài chục phiên đồng thời) số sessionId
    // phân biệt trong suốt vòng đời app là nhỏ, không đáng để thêm logic dọn có nguy cơ race
    // (xoá đúng lúc 1 luồng khác vừa lấy lock cũ từ map).
    private final ConcurrentHashMap<UUID, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ResolveNextPracticeQuestionUseCase.class);

    private static final String CLUSTER_LOCK_KEY_PREFIX = "practice:next-question:";

    /**
     * Biên an toàn cộng vào ngân sách sinh câu để ra TTL của khoá.
     *
     * <p>TTL BẮT BUỘC dài hơn thời gian chạy tối đa. Ngắn hơn thì khoá hết hạn trong lúc chủ khoá
     * vẫn đang sinh câu, người thứ hai vào được, và ta quay lại đúng lỗi đang đi sửa -- lần này im
     * lặng hơn, vì nhìn vào thì thấy "đã có khoá rồi".
     */
    private static final Duration CLUSTER_LOCK_MARGIN = Duration.ofSeconds(30);

    /** Nhịp hỏi lại khoá. Người chờ là đường hiếm, không cần backoff cho phức tạp. */
    private static final Duration LOCK_POLL_INTERVAL = Duration.ofMillis(200);

    private final ResolveNextPracticeQuestionClaimService claimService;
    private final PracticeQuestionSelectionService selectionService;
    private final ResolveNextPracticeQuestionPersistenceService persistenceService;
    private final CacheManagerPort cacheManagerPort;
    private final PracticeGenerationProperties generationProperties;

    public ResolveNextPracticeQuestionUseCase(
            ResolveNextPracticeQuestionClaimService claimService,
            PracticeQuestionSelectionService selectionService,
            ResolveNextPracticeQuestionPersistenceService persistenceService,
            CacheManagerPort cacheManagerPort,
            PracticeGenerationProperties generationProperties) {
        this.claimService = claimService;
        this.selectionService = selectionService;
        this.persistenceService = persistenceService;
        this.cacheManagerPort = cacheManagerPort;
        this.generationProperties = generationProperties;
    }

    public Result execute(UUID sessionId) {
        // Cổng TRONG: khoá JVM. Giữ lại dù đã có khoá Redis vì hai lời gọi trùng CÙNG pod chặn
        // nhau ngay tại đây, không tốn một vòng mạng nào -- mà từ khi bật sticky session thì đa số
        // trùng lặp đúng là cùng pod. Hành vi trong phạm vi một pod vì vậy giống hệt bản cũ.
        var lock = sessionLocks.computeIfAbsent(sessionId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return executeWithClusterLock(sessionId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cổng NGOÀI: khoá phân tán, chặn trùng giữa các pod.
     *
     * <p>Người trượt khoá phải CHỜ chứ không được trả lỗi: chờ xong thì {@code claim()} thấy câu đã
     * được ghi và trả về qua nhánh {@code idempotentQuestion} -- đúng thứ khoá JVM đang làm hôm
     * nay, và đúng hợp đồng Python trông đợi ("a retry returns that SAME item instead of picking a
     * new one").
     *
     * <p>Nhả khoá nằm ở {@code finally} NGOÀI {@code doExecute}, tức sau khi transaction ghi câu đã
     * commit. Nhả sớm hơn là người chờ chạy {@code claim()} lúc câu chưa commit rồi lại sinh câu
     * thứ hai -- đúng lỗi vừa sửa.
     */
    private Result executeWithClusterLock(UUID sessionId) {
        var key = CLUSTER_LOCK_KEY_PREFIX + sessionId;
        // Token riêng mỗi lượt để lúc nhả còn chứng minh được quyền sở hữu.
        var token = UUID.randomUUID().toString();
        var ttl = clusterLockTtl();
        var deadline = Instant.now().plus(ttl).plusSeconds(5);

        while (true) {
            if (token.equals(cacheManagerPort.saveIfAbsentAndGet(key, token, ttl))) {
                try {
                    return doExecute(sessionId);
                } finally {
                    cacheManagerPort.deleteIfValueMatches(key, token);
                }
            }
            if (Instant.now().isAfter(deadline)) {
                // Quá cả TTL mà vẫn không lấy được nghĩa là chủ khoá coi như đã chết (khoá không
                // sống lâu hơn TTL được). Chạy tiếp không khoá thay vì ném lỗi: tới nước này ném
                // lỗi là kết thúc phiên luyện của học sinh, còn nguy cơ sinh trùng thì đã rất nhỏ.
                LOGGER.warn(
                    "Chờ quá {} mà không lấy được khoá {} -- chạy tiếp không khoá.", ttl, key
                );
                return doExecute(sessionId);
            }
            sleepQuietly();
        }
    }

    /**
     * TTL suy TỪ cấu hình, không viết cứng: ai chỉnh
     * {@code PERSONALIZATION_ONLINE_GENERATION_BUDGET} lên thì TTL tự dài theo. Viết cứng một con
     * số ở đây là gài mìn cho lần chỉnh cấu hình sau.
     */
    private Duration clusterLockTtl() {
        return generationProperties.onlineBudget().plus(CLUSTER_LOCK_MARGIN);
    }

    /**
     * Bị ngắt thì đặt lại cờ rồi chờ tiếp, KHÔNG thoát sớm -- {@code ReentrantLock.lock()} mà nó
     * thay thế cũng không ngắt được. Vòng lặp vẫn dừng nhờ {@code deadline}.
     */
    private void sleepQuietly() {
        try {
            Thread.sleep(LOCK_POLL_INTERVAL.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private Result doExecute(UUID sessionId) {
        var claim = claimService.claim(sessionId);
        if (claim.idempotentQuestion() != null) {
            return Result.ok(
                toPayload(
                    claim.idempotentQuestion(),
                    claim.idempotentSlot(),
                    claim.budgetSeconds() - claim.spokenSeconds()
                ),
                claim.spokenSeconds(),
                claim.budgetSeconds()
            );
        }
        if (claim.earlyExitReason() != null) {
            return Result.noMoreQuestions(
                claim.earlyExitReason(), claim.spokenSeconds(), claim.budgetSeconds()
            );
        }

        var selection = selectionService
            .resolveNextQuestion(
                claim.topic(), claim.studentId(), claim.focus(),
                claim.targetBandOrder(), claim.alreadyChosen()
            )
            .orElse(null);
        if (selection == null) {
            return Result.noMoreQuestions(
                "pool_exhausted", claim.spokenSeconds(), claim.budgetSeconds()
            );
        }

        // Không còn chốt ngân sách ở đây: nó đã chuyển lên ResolveNextPracticeQuestionClaimService,
        // chạy TRƯỚC resolveNextQuestion. Đặt sau như cũ nghĩa là có thể sinh câu bằng LLM
        // (10-40 giây) rồi vứt đi vì hết ngân sách.
        var question = selection.question();

        persistenceService.persist(claim.studentId(), claim.practicePaperId(), selection);
        return Result.ok(
            toPayload(question, selection.slot(), claim.budgetSeconds() - claim.spokenSeconds()),
            claim.spokenSeconds(),
            claim.budgetSeconds()
        );
    }

    /**
     * May đo thời lượng câu hỏi theo ngân sách CÒN LẠI của phiên.
     *
     * Khi phần còn lại không đủ cho một câu cỡ bình thường, thay vì từ chối phát câu (bỏ
     * phí phần thời gian đó) thì cắt câu cho vừa: trần = đúng số giây còn lại, sàn = một
     * nửa. Học sinh vẫn nói được thêm một lượt trọn vẹn theo cỡ nhỏ hơn, rồi phiên đóng.
     *
     * KHÔNG ghi đè vào bản ghi câu hỏi trong kho -- câu hỏi dùng chung cho mọi học sinh,
     * sửa nó là làm hỏng cho người khác. Chỉ đổi con số gửi xuống cho phiên NÀY.
     */
    private QuestionPayload toPayload(PracticeQuestion question, int slot, int remainingSeconds) {
        // > 1 chứ không > 0: còn đúng 1 giây thì cắt ra sàn = trần = 1, mà sàn phải NHỎ HƠN
        // trần -- bằng nhau thì SignalNode coi mọi câu trả lời là chưa đạt và hỏi mãi.
        // Dưới ngưỡng đó cứ giữ nguyên số của câu; phiên sẽ đóng ở chốt ngân sách lần sau.
        var last = remainingSeconds > 1 && remainingSeconds < question.getMaxResponseSeconds();
        var maxSeconds = last ? remainingSeconds : question.getMaxResponseSeconds();
        var minSeconds = Math.max(1, Math.min(
            last ? remainingSeconds / 2 : question.getMinResponseSeconds(),
            maxSeconds - 1
        ));
        return new QuestionPayload(
            question.getId(),
            slot,
            question.getQuestionText(),
            question.getTargetCriterionCode(),
            question.getTargetSubAttribute(),
            question.getDifficultyRank(),
            maxSeconds,
            minSeconds,
            parseIdeas(question.getSuggestedIdeasJson()),
            last
        );
    }

    private List<String> parseIdeas(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return new tools.jackson.databind.json.JsonMapper()
                .readValue(value, new tools.jackson.core.type.TypeReference<List<String>>() {
                });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
