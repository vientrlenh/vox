package com.sep.vox.application.port.input.service;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;

/**
 * Tops up the practice topic bank when the suggestion list is sparse -- runs in the BACKGROUND.
 *
 * Why not generate right in the request: {@code TopicSuggestionService.synchronousOffers} calls the
 * LLM to propose topics and then indexes them into the vector store, taking tens of seconds. While the bank is still empty (a new
 * student, or data just rebuilt), EVERY time the topic-picker screen opens it falls onto that path, so the
 * first request after finishing the quiz always times out -- right when the student is most excited.
 *
 * Change the shape instead of raising the cap: return immediately what already exists (even empty), generate in the background,
 * the client shows "compiling, come back in 1-2 minutes". Topics, once generated, are written straight into the bank
 * ({@code createTopic} inside synchronousOffers), so the next time the screen opens the normal ranking
 * path sees them by itself -- no phase two or re-ask endpoint needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicOfferBackfillService {

    /**
     * Once the bank is large enough, slow generation down. Below the threshold is the build-up phase -- a new student or freshly
     * rebuilt data -- and the bank must be wide enough for ranking to have something to choose from (ε-greedy even requires
     * {@code ranked.size() > 10} before it runs). Above the threshold each session consumes only one topic,
     * so generating a lot just bloats the bank: measured after 6 sessions at 8 per session, that gave 56 topics, 37 of which
     * were never offered even once.
     */
    private static final long POOL_TARGET = 40;

    /** Generation pace while the bank is thin -- prioritize filling it quickly. */
    private static final int BACKFILL_COUNT_BUILDING = 8;

    /**
     * Generation pace once the bank has reached {@link #POOL_TARGET} -- only enough to cover wear.
     *
     * <p>Still faster than the consumption rate (1 topic/session), so the bank does not shrink, yet it does not dilute into
     * near-identical variants: every batch is generated from the same six interest scores, which barely change, so
     * the denser the generation, the more a batch resembles the previous one and grinds itself against its own duplicate filter.
     */
    private static final int BACKFILL_COUNT_STEADY = 2;

    /**
     * Prevents duplicate runs across the WHOLE CLUSTER, not only in the RAM of one pod.
     *
     * <p>The previous version was {@code ConcurrentHashMap.newKeySet()} -- it only saw the runs of
     * its own pod. With 3 replicas behind an ALB that balances round-robin, three consecutive
     * {@code practiceTopicOffers} calls landing on three pods are three topic-generation runs in parallel
     * for the same student: the LLM cost triples, and worse, all three load {@code activeNameCards()}
     * BEFORE the other two have written, so they do not see each other -- the duplicate check by name is ineffective and only
     * Chroma on the Python side helps.
     *
     * <p>(The call path from {@code PracticeSessionClosedHandler.afterClosed} was already safe: the session cleanup
     * job uses {@code FOR UPDATE SKIP LOCKED}, so only one pod can hold that session.)
     */
    private static final String IN_FLIGHT_KEY_PREFIX = "practice:backfill:";

    /**
     * Clearly longer than one generation run so the lock does not expire midway: the HTTP timeout to the agents is 45
     * seconds, plus the time to write the topics and index them into the vector store.
     *
     * <p>This is only a safety net for the case where a pod dies midway -- the normal path always removes
     * the lock in {@code finally}.
     */
    private static final Duration IN_FLIGHT_TTL = Duration.ofMinutes(2);

    private final TopicSuggestionService topicSuggestionService;
    private final PracticeTopicRepository practiceTopicRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final CacheManagerPort cacheManagerPort;
    private final PracticeGenerationExecutorPort practiceGenerationExecutorPort;

    public void backfillAsync(UUID studentId) {
        practiceGenerationExecutorPort.runAsync(() -> backfill(studentId));
    }

    /**
     * Each run gets its OWN token: releasing the lock must prove ownership, otherwise a run that
     * overran its deadline could delete the lock of the next run by mistake.
     *
     * <p>If some pod (including this one) is already running, this run is skipped, exactly as in the old version.
     *
     * <p>The error is swallowed on purpose: this is background work that enriches the bank. If it fails the topic
     * selection screen still shows what it has, and the next open will try again.
     */
    private void backfill(UUID studentId) {
        if (studentId == null) {
            return;
        }
        if (!hasCompletedInterestQuiz(studentId)) {
            log.debug("Skipping the topic top-up: student {} has not submitted the interest quiz.", studentId);
            return;
        }
        String key = IN_FLIGHT_KEY_PREFIX + studentId;
        String token = UUID.randomUUID().toString();
        if (!token.equals(cacheManagerPort.saveIfAbsentAndGet(key, token, IN_FLIGHT_TTL))) {
            return;
        }
        try {
            backfillOffers(studentId);
        } catch (RuntimeException exception) {
            log.warn("Topic top-up failed for student {}", studentId, exception);
        } finally {
            cacheManagerPort.deleteIfValueMatches(key, token);
        }
    }

    /**
     * The pool is counted BEFORE every run, not cached: the bank is shared and many students push it
     * up, so a value read at startup would be wrong after a few sessions.
     */
    private void backfillOffers(UUID studentId) {
        long pool = practiceTopicRepository.countOfferablePool();
        int requested = pool < POOL_TARGET ? BACKFILL_COUNT_BUILDING : BACKFILL_COUNT_STEADY;
        List<PracticePlanningResponses.PracticeTopicOffer> created = topicSuggestionService.synchronousOffers(studentId, requested);
        log.info(
            "Topped up {}/{} topics for student {} (the bank now has {}).",
            created.size(), requested, studentId, pool
        );
    }

    /**
     * If the interest quiz has not been submitted, generate NOTHING -- this is the other half of the gate placed in
     * ViewPracticeTopicOffersUseCase, and it is the MORE IMPORTANT half.
     *
     * <p>Blocking only on the use case side is not enough: it returns an empty list, and an empty list makes
     * GraphQLPracticePlanningController see `offers.size() < 3` and call straight into {@link #backfill(UUID)}. The
     * result is even worse than before -- the offer batch is blocked but the pool still gets filled.
     *
     * <p>The generation run at that point sends an EMPTY `interestScores` to the LLM (see
     * TopicSuggestionService.synchronousOffers), so the topics that come back are generic and
     * tied to nobody. But practice_topics is a pool SHARED by the whole system and those rows stay
     * there permanently: one login by an account that has not done the quiz is enough to shape
     * the pool for every student afterwards.
     *
     * <p>The call path from PracticeSessionClosedHandler in theory always has a quiz already (a session
     * must be practised to the end before reaching it), so for it this condition is harmless.
     */
    private boolean hasCompletedInterestQuiz(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getQuizCompletedAt() != null)
            .orElse(false);
    }
}
