package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.repository.PendingEvaluationResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;

/**
 * Flushes grading for the questions a student spoke but left unfinished, when the session closes.
 *
 * Why needed: grading is only triggered on the LAST turn of a question
 * ({@code SubmitPracticeTurnUseCase} fires the event when {@code questionComplete}). A student who
 * speaks one turn and then drops off the network or closes the app never finishes the follow-up chain, so that question
 * is never graded -- while the quota HAS BEEN deducted, the turn HAS BEEN recorded, and the mistake HAS been shown on
 * screen. Real effort that the system treats as if it did not exist.
 *
 * The knock-on effect reaches further than the score: with no grading there is no
 * {@code weakness_observation}, so the weakness profile stands still, so the criterion rotation
 * has nothing new to rotate.
 *
 * The session score does NOT count these questions -- see {@code question_complete} in
 * {@code SpringDataPracticeItemEvaluationRepository}. Grading an unfinished question against the rubric of a
 * complete one is bound to score low; taking a signal from it is fine, scoring with it is not fair.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeGradingFlushService {

    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeEvaluationRequestFactory evaluationRequestFactory;
    private final ExternalEventPublisherPort eventPublisher;

    /**
     * Sweeps CLOSED sessions that still have ungraded turns and flushes grading for them.
     *
     * Flushing at session close covers neither of two groups: sessions closed before that mechanism existed, and sessions
     * where firing the event failed. Without a re-sweep they stay orphaned forever -- the session score
     * is missing, the weakness profile is missing, and the summary screen says "grading" and never finishes.
     *
     * @return the number of questions fired again.
     */
    public int sweepEndedSessions(Instant since) {
        List<UUID> sessionIds = practiceItemResponseRepository.findEndedSessionsWithUngradedResponses(since);
        int total = 0;
        for (UUID sessionId : sessionIds) {
            total += flush(sessionId);
        }
        if (total > 0) {
            log.info("Re-sweep: flushing grading of {} orphaned questions in {} closed sessions.", total, sessionIds.size());
        }
        return total;
    }

    /**
     * The COLD window: if a grading request was sent within this window, treat it as running and do not resend.
     *
     * 15 minutes is chosen from the measured grading time (~3.5 minutes/question) plus queueing time when many
     * questions arrive at once. Shorter would create duplicates again -- exactly what this column exists to block; longer
     * would make a truly lost request wait longer than necessary.
     */
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(15);

    /**
     * The cap on grading attempts for one question. A failure caused by DATA (broken audio, empty transcript, a question so short
     * that no prompt can be built) fails identically however many times you try -- firing endlessly only burns
     * LLM money and keeps the summary screen spinning. Three attempts are enough to get past a transient error (network congestion,
     * rate limit) without turning a permanent error into an endless loop.
     */
    public static final int MAX_GRADING_ATTEMPTS = 3;

    /** @return the number of questions that had a supplementary grading request fired. */
    public int flush(UUID sessionId) {
        List<PendingEvaluationResponse> pending = practiceItemResponseRepository.findResponsesAwaitingFlush(
            sessionId, Instant.now().minus(REQUEST_COOLDOWN), MAX_GRADING_ATTEMPTS
        );
        int flushed = 0;
        for (PendingEvaluationResponse row : pending) {
            if (flushOne(sessionId, row)) {
                flushed++;
            }
        }
        if (flushed > 0) {
            log.info("Flushed grading of {} unfinished questions of session {}.", flushed, sessionId);
        }
        return flushed;
    }

    private boolean flushOne(UUID sessionId, PendingEvaluationResponse row) {
        try {
            eventPublisher.publish(evaluationRequestFactory.build(
                sessionId, row.getResponseId(), row.getQuestionId()
            ));
            // Stamp AFTER the publish succeeds: stamping first and then failing to publish would lock
            // that question out of the sweep for the whole cooldown window, losing a rescue cycle outright.
            practiceItemResponseRepository.markGradingRequested(
                row.getResponseId(), Instant.now()
            );
            return true;
        } catch (RuntimeException exception) {
            // One failing question must not drag down the session close with it: closing the session is the
            // student's action, while flushing the grading is the system's housekeeping.
            log.warn(
                "Could not flush grading for question {} of session {}.",
                row.getQuestionId(), sessionId, exception
            );
            return false;
        }
    }
}
