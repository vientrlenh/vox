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
 * Resolves EXACTLY ONE next MAIN question while the practice session is running -- called from Python (internal,
 * protected by PracticeInternalSecretFilter) when decision.should_continue == False. See package 11 section 2.4
 * step 4.
 *
 * NOT @Transactional at this layer: selectionService.resolveNextQuestion (called inside doExecute) may call the LLM
 * to generate a question directly (band 4, 10-20s) when the topic is short of questions -- holding 1 DB
 * transaction/connection open the whole time once exhausted the HikariCP pool under real load (see
 * BuildPracticePaperUseCase, the same class of bug, fixed in the same round).
 *
 * Serializing duplicate calls for the SAME session (Python's timeout-retry-8s racing the original call that is still
 * running, see practice_session_client.py) used to rely on SELECT ... FOR UPDATE held across the whole method -- now
 * replaced by a JVM lock keyed by sessionId, keeping an equivalent scope (enough because Python always retries into
 * this same vox-app instance) but not occupying a DB connection while waiting on the network. The real transaction
 * (FOR UPDATE + read, then write) is split into 2 separate beans
 * (ResolveNextPracticeQuestionClaimService / ...PersistenceService) wrapping exactly the DB part, with the slow LLM
 * call in the MIDDLE, outside every transaction.
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
     * The OUTER gate: a distributed lock, blocking duplicates between pods.
     *
     * <p>Whoever loses the lock must WAIT rather than return an error: once done waiting, {@code claim()} sees the
     * question was already written and returns it through the {@code idempotentQuestion} branch -- exactly what the
     * JVM lock does today, and exactly the contract Python expects ("a retry returns that SAME item instead of
     * picking a new one").
     *
     * <p>The lock is released in the {@code finally} OUTSIDE {@code doExecute}, i.e. after the transaction that
     * writes the question has committed. Releasing earlier lets a waiter run {@code claim()} while the question is
     * not yet committed and generate a second question -- exactly the bug that was just fixed.
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
            "Waited more than {} without getting lock {} -- continuing without the lock.", ttl, key
        );
        return doExecute(sessionId);
    }

    /**
     * The TTL is DERIVED from configuration, not hard-coded: whoever raises
     * {@code PERSONALIZATION_ONLINE_GENERATION_BUDGET} makes the TTL grow with it. Hard-coding a number here
     * would plant a mine for the next configuration change.
     */
    private Duration clusterLockTtl() {
        return generationConfig.onlineBudget().plus(CLUSTER_LOCK_MARGIN);
    }

    /**
     * When interrupted, restore the flag and keep waiting, do NOT leave early -- the {@code ReentrantLock.lock()} it
     * replaces cannot be interrupted either. The loop still ends because of {@code deadline}.
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
     * Tailors the question duration to the session's REMAINING budget.
     *
     * When the remainder is not enough for a normal-sized question, instead of refusing to issue the question (wasting
     * that time) it cuts the question to fit: ceiling = exactly the seconds left, floor = half of it. The student can
     * still speak one more complete turn at a smaller size, and then the session closes.
     *
     * Does NOT write over the question record in the pool -- the question is shared by every student, and editing it
     * would break it for others. Only the number sent down for THIS session is changed.
     */
    private QuestionPayload toPayload(PracticeQuestion question, int slot, int remainingSeconds) {
        // > 1 and not > 0: with exactly 1 second left the cut gives floor = ceiling = 1, but the floor must be SMALLER
        // than the ceiling -- when they are equal SignalNode treats every answer as not good enough and asks forever.
        // Below that threshold just keep the question's own number; the session closes at the next budget check.
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
