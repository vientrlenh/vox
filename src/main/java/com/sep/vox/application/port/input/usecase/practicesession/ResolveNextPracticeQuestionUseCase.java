package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionClaimService;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionPersistenceService;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.PracticeGenerationConfigPort;
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
@RequiredArgsConstructor
@Slf4j
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
         * The LAST question of the session: the remaining budget is not enough for a normal-sized question, so
         * this question is tailored to fit exactly what is left. Once it is answered the session closes and no
         * further question is asked -- see {@link #toPayload} for how it is computed.
         */
        boolean lastQuestion) {
    }

    /**
     * {@code sessionSpokenSeconds}/{@code sessionBudgetSeconds}: the pair of numbers for the progress bar on the
     * student's machine. They travel with the question because Python sends them to the client RIGHT when the
     * session opens -- the first turn is not submitted yet, so there is no SubmitTurnResult to take the budget from.
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

    // Old entries are not cleaned up: at the current scale (demo, a few dozen concurrent sessions) the number of
    // distinct sessionIds over the whole app lifetime is small, not worth adding cleanup logic that risks a race
    // (removing an entry just when another thread has taken the old lock from the map).
    private final ConcurrentHashMap<UUID, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    private static final String CLUSTER_LOCK_KEY_PREFIX = "practice:next-question:";

    /**
     * Safety margin added to the question-generation budget to get the lock TTL.
     *
     * <p>The TTL MUST be longer than the maximum run time. If it is shorter, the lock expires while its owner
     * is still generating, a second caller gets in, and we are back to the very bug being fixed -- only quieter
     * this time, because a look at it shows "there is already a lock".
     */
    private static final Duration CLUSTER_LOCK_MARGIN = Duration.ofSeconds(30);

    /** Interval for asking for the lock again. Waiting is a rare path, so no backoff is needed. */
    private static final Duration LOCK_POLL_INTERVAL = Duration.ofMillis(200);

    private final ResolveNextPracticeQuestionClaimService claimService;
    private final PracticeQuestionSelectionService selectionService;
    private final ResolveNextPracticeQuestionPersistenceService persistenceService;
    private final CacheManagerPort cacheManagerPort;
    private final PracticeGenerationConfigPort generationConfig;
    private final JsonSerializationPort jsonSerializationPort;

    public Result execute(UUID sessionId) {
        // INNER gate: the JVM lock. Kept even though the Redis lock exists, because two duplicate calls on
        // the SAME pod block each other right here without a network round trip -- and since sticky
        // sessions were enabled, most duplicates are on the same pod. Behaviour within one pod is
        // therefore identical to the old version.
        ReentrantLock lock = sessionLocks.computeIfAbsent(sessionId, ignored -> new ReentrantLock());
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
        String key = CLUSTER_LOCK_KEY_PREFIX + sessionId;
        // A token of its own per attempt so that the release can prove ownership.
        String token = UUID.randomUUID().toString();
        Duration ttl = clusterLockTtl();
        Instant deadline = Instant.now().plus(ttl).plusSeconds(5);

        while (true) {
            if (token.equals(cacheManagerPort.saveIfAbsentAndGet(key, token, ttl))) {
                return executeHoldingClusterLock(sessionId, key, token);
            }
            if (Instant.now().isAfter(deadline)) {
                return executeAfterLockWaitTimedOut(sessionId, key, ttl);
            }
            sleepQuietly();
        }
    }

    private Result executeHoldingClusterLock(UUID sessionId, String key, String token) {
        try {
            return doExecute(sessionId);
        } finally {
            cacheManagerPort.deleteIfValueMatches(key, token);
        }
    }

    private Result executeAfterLockWaitTimedOut(UUID sessionId, String key, Duration ttl) {
        // Still not acquired after a whole TTL means the lock owner is considered dead (a lock cannot
        // outlive its TTL). Carry on without the lock instead of throwing: at this point throwing ends the
        // student's practice session, while the risk of generating a duplicate is already very small.
        log.warn(
            "Chờ quá {} mà không lấy được khoá {} -- chạy tiếp không khoá.", ttl, key
        );
        return doExecute(sessionId);
    }

    /**
     * TTL suy TỪ cấu hình, không viết cứng: ai chỉnh
     * {@code PERSONALIZATION_ONLINE_GENERATION_BUDGET} lên thì TTL tự dài theo. Viết cứng một con
     * số ở đây là gài mìn cho lần chỉnh cấu hình sau.
     */
    private Duration clusterLockTtl() {
        return generationConfig.onlineBudget().plus(CLUSTER_LOCK_MARGIN);
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
        ResolveNextPracticeQuestionClaimService.Claim claim = claimService.claim(sessionId);
        Result resultWithoutSelection = resultWithoutSelection(claim);
        if (resultWithoutSelection != null) {
            return resultWithoutSelection;
        }
        return selectAndPersistNextQuestion(claim);
    }

    /** Result for a claim that needs no new selection (idempotent retry or early exit), otherwise null. */
    private Result resultWithoutSelection(ResolveNextPracticeQuestionClaimService.Claim claim) {
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
        return null;
    }

    /**
     * The budget check is no longer here: it moved up to ResolveNextPracticeQuestionClaimService and
     * runs BEFORE resolveNextQuestion. Placing it after, as before, could generate a question with the
     * LLM (10-40 seconds) and then throw it away because the budget is exhausted.
     */
    private Result selectAndPersistNextQuestion(ResolveNextPracticeQuestionClaimService.Claim claim) {
        PracticeQuestionSelectionService.NextQuestionSelection selection = selectionService
            .resolveNextQuestion(
                claim.topic(), claim.studentId(), claim.focus(),
                claim.targetBandOrder(), claim.targetBandCount(),
                claim.targetFrameworkVersionId(), claim.alreadyChosen()
            )
            .orElse(null);
        if (selection == null) {
            return Result.noMoreQuestions(
                "pool_exhausted", claim.spokenSeconds(), claim.budgetSeconds()
            );
        }
        PracticeQuestion question = selection.question();
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
        boolean last = remainingSeconds > 1 && remainingSeconds < question.getMaxResponseSeconds();
        int maxSeconds = last ? remainingSeconds : question.getMaxResponseSeconds();
        int minSeconds = Math.max(1, Math.min(
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
            return jsonSerializationPort.toStringList(value);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }
}
