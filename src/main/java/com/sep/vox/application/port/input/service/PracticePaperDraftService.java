package com.sep.vox.application.port.input.service;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperDraft;

/**
 * Orchestrates two-phase paper building.
 *
 * Why needed: building a paper falls to tier 4 (asking AI to generate new questions) when the bank has no suitable questions,
 * taking 10-20s -- too long to hold an HTTP request (client timeout, a Tomcat thread + a DB connection
 * held along with it). So it is split: phase 1 starts and returns immediately, phase 2 the client asks again.
 *
 * It still WAITS BRIEFLY ({@link #FAST_PATH_WAIT}) before returning PREPARING: when the bank already has questions (tier 1,
 * a few dozen ms -- the common path once the bank is full) it returns READY directly, so the student does not
 * suffer an extra round of asking again just to receive what was already ready.
 *
 * Takes a {@code Supplier} instead of calling the use case itself so there is no reverse dependency from the service layer up to
 * the use-case layer -- this service only handles the draft lifecycle + background execution, the paper building is passed in by the caller.
 *
 * The draft is stored in REDIS, not the DB: losing it when Redis is lost is acceptable (the student clicks again and
 * it is done, practice_papers itself is already in the DB with a 10-minute reservation), in exchange for not having to add a
 * table + migration for something that lives a few dozen seconds.
 *
 * <p>FIXED 2026-08-11 -- it used to be a {@code ConcurrentHashMap} in RAM. With 3 replicas behind an ALB
 * balancing round-robin, the draft only existed on the pod that created it: a client creating a draft on pod A and then
 * asking again got routed to pod B, whose map was empty -> 404 "Paper-building session not found". Hitting the original
 * pod again is only 1/3 likely so the error looked intermittent rather than fully broken.
 *
 * <p>The background part does NOT need to change: whichever pod starts it builds it and then writes the result into
 * Redis, and whichever pod reads it sees it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PracticePaperDraftService {

    private static final Duration FAST_PATH_WAIT = Duration.ofSeconds(2);
    private static final Duration DRAFT_TTL = Duration.ofMinutes(10);

    private static final String DRAFT_KEY_PREFIX = "practice:draft:";

    /**
     * PUBLIC because Jackson must be able to rebuild this record when reading it from Redis.
     *
     * <p>The {@code createdAt} field was dropped: it only served {@code evictExpired()}, and the Redis TTL
     * now does that job, and does it better, because the old version only cleaned up when someone created a new draft.
     */
    public record Entry(UUID studentId, PracticePaperDraft draft) {
    }

    private final PracticeGenerationExecutorPort practiceGenerationExecutorPort;
    private final CacheManagerPort cacheManagerPort;

    public PracticePaperDraft start(UUID studentId, Supplier<PracticePaper> buildPaper) {
        UUID draftId = UUID.randomUUID();
        store(draftId, studentId, PracticePaperDraft.preparing(draftId));

        // Runs on its own executor, NOT on the request thread: that is what lets OSIV release the DB
        // connection instead of holding it for the whole LLM wait (the executor wraps the SecurityContext, so
        // the use case inside can still read the current user).
        CompletableFuture<PracticePlanningResponses.PracticePaper> future = practiceGenerationExecutorPort.supplyAsync(buildPaper);

        try {
            PracticePlanningResponses.PracticePaper paper = future.get(FAST_PATH_WAIT.toMillis(), TimeUnit.MILLISECONDS);
            return store(draftId, studentId, PracticePaperDraft.ready(draftId, paper));
        } catch (TimeoutException timeout) {
            return finishInBackground(draftId, studentId, future);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return store(draftId, studentId,
                PracticePaperDraft.failed(draftId, "Paper building was interrupted, please try again.", null));
        } catch (ExecutionException failed) {
            return store(draftId, studentId, failure(draftId, failed));
        }
    }

    /** New questions still have to be generated -- let it keep running in the background, the client asks again later. */
    private PracticePaperDraft finishInBackground(
            UUID draftId, UUID studentId, CompletableFuture<PracticePlanningResponses.PracticePaper> future) {
        future.whenComplete((paper, error) -> store(
            draftId,
            studentId,
            error == null
                ? PracticePaperDraft.ready(draftId, paper)
                : failure(draftId, error)
        ));
        return PracticePaperDraft.preparing(draftId);
    }

    public PracticePaperDraft get(UUID studentId, UUID draftId) {
        Entry entry = cacheManagerPort.get(DRAFT_KEY_PREFIX + draftId, Entry.class);
        if (entry == null) {
            throw new ResourceNotFoundException("Paper-building session not found, please choose the topic again.");
        }
        if (!entry.studentId().equals(studentId)) {
            throw new ForbiddenException("You do not have permission to view this paper-building session.");
        }
        return entry.draft();
    }

    private PracticePaperDraft store(UUID draftId, UUID studentId, PracticePaperDraft draft) {
        // The TTL is reset on every write. Intended: the expiry is counted from the last update, and the final state
        // (READY/FAILED) is what the client needs to be able to read in time.
        cacheManagerPort.save(DRAFT_KEY_PREFIX + draftId, new Entry(studentId, draft), DRAFT_TTL);
        return draft;
    }

    private static PracticePaperDraft failure(UUID draftId, Throwable error) {
        log.warn("Paper building failed for draft {}", draftId, error);
        FailureReason reason = readableReason(error);
        return PracticePaperDraft.failed(draftId, reason.message(), reason.errorCode());
    }

    /** A (message, error code) pair derived from the cause of the failure. */
    private record FailureReason(String message, String errorCode) {
    }

    /** Keeps the message of a business error (quota exhausted, topic without questions...) because those messages are
     * written to be shown to the student as they are; an unknown error gets a generic sentence that leaks no technical
     * detail. The errorCode is set only for BusinessRuleViolationException -- these are errors the student should NOT
     * retry (the quota does not refill by itself), unlike the other business errors, which may disappear on a retry. */
    private static FailureReason readableReason(Throwable error) {
        Throwable cause = error instanceof ExecutionException && error.getCause() != null
            ? error.getCause()
            : error;
        if (cause instanceof BusinessRuleViolationException) {
            return new FailureReason(cause.getMessage(), "BUSINESS_RULE_VIOLATION");
        }
        if (cause instanceof ResourceNotFoundException || cause instanceof IllegalArgumentException) {
            return new FailureReason(cause.getMessage(), null);
        }
        return new FailureReason("Cannot build a practice paper right now, please try again.", null);
    }

}
