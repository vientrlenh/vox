package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticeSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;

/**
 * Phase 1 (lock + read + check idempotency/budget) of ResolveNextPracticeQuestionUseCase --
 * split out into its own @Transactional bean to hold FOR UPDATE + the DB connection only for
 * this quick read, NOT stretched across the slow LLM call (selectionService.resolveNextQuestion
 * tier 4) as in the old code. Serializing duplicate calls for the same session is now done by a JVM lock in
 * ResolveNextPracticeQuestionUseCase; FOR UPDATE here only protects this short read
 * from overwrites elsewhere, and no longer has to be held across an external call.
 */
@Service
@RequiredArgsConstructor
public class ResolveNextPracticeQuestionClaimService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeTopicRepository topicRepository;
    private final PracticeQuestionRepository questionRepository;
    private final PracticePaperItemRepository paperItemRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeQuestionSelectionService selectionService;
    private final PracticeTopicOfferEnrichmentService enrichmentService;

    /**
     * Exactly 1 of 3 meaningful outcomes:
     * - idempotentQuestion != null: the LATEST question was already resolved earlier (by this call or
     *   1 earlier call that timed out) but has no response yet -- return it AS IS, do not resolve a new one.
     * - earlyExitReason != null: stop right away (quota exhausted), no need to call resolveNextQuestion.
     * - both null: continue to phase 2 (call resolveNextQuestion, may be slow) and then persist.
     */
    public record Claim(
        UUID studentId,
        UUID practicePaperId,
        PracticeTopic topic,
        PracticeFocusInfo focus,
        List<PracticeQuestion> alreadyChosen,
        PracticeQuestion idempotentQuestion,
        int idempotentSlot,
        String earlyExitReason,
        /**
         * The pair of numbers for the "spoken / budget" progress bar on the student's device. Differs from {@code
         * usedSeconds}/{@code maxSeconds} just above in the MOST IMPORTANT way: usedSeconds is the
         * ESTIMATED budget of the questions already issued (used to decide whether to issue another question),
         * while spokenSeconds is the number of seconds the student REALLY spoke (exactly what is deducted from quota).
         * On real data the two numbers differ greatly -- 45 seconds budgeted for 16 seconds spoken.
         */
        int spokenSeconds,
        int budgetSeconds,
        /**
         * The band order the student chose when building the paper, read back from the session. Previously this place
         * re-estimated the band on every turn (measured band + performance EMA + most recent abandoned attempt), so difficulty
         * could drift mid-session without the student understanding why.
         */
        int targetBandOrder,
        /** The band count of the framework that targetBandOrder belongs to -- see frameworkBandCountOfBand. */
        int targetBandCount,
        /** The framework the target band belongs to -- the ladder sent to Python must follow exactly this framework. */
        UUID targetFrameworkVersionId) {
    }

    /**
     * Below this threshold no new question is issued. Not "enough for one complete question" --
     * just the point below which issuing another question is pointless: the student runs out of time before saying anything.
     */
    private static final int MINIMUM_USEFUL_TURN_SECONDS = 10;

    @Transactional
    public Claim claim(UUID sessionId) {
        PracticeSession session = lockInProgressSession(sessionId);
        Progress progress = measureProgress(session);

        List<UUID> alreadyChosenIds = paperItemRepository.findQuestionIdsForPaper(session.getPracticePaperId());
        Optional<Claim> idempotentClaim = findIdempotentClaim(session, progress, alreadyChosenIds);
        if (idempotentClaim.isPresent()) {
            return idempotentClaim.get();
        }

        if (isBudgetExhausted(progress)) {
            return claimOf(session, progress, null, null, null, null, 0, "budget_exhausted");
        }

        PracticeTopic topic = topicRepository.findTopicById(session.getChosenPracticeTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Practice topic not found."));
        PracticeFocusInfo focus = selectionService.resolveFocus(session.getStudentId(), null);
        List<PracticeQuestion> alreadyChosen = questionRepository.findByIds(alreadyChosenIds);

        return claimOf(session, progress, topic, focus, alreadyChosen, null, 0, null);
    }

    /**
     * FOR UPDATE: it now only has to protect this short read/check section -- the JVM lock in
     * ResolveNextPracticeQuestionUseCase is what serializes the whole slow external call.
     */
    private PracticeSession lockInProgressSession(UUID sessionId) {
        PracticeSession session = practiceSessionRepository.findByIdForUpdate(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Practice session not found."));
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalArgumentException("The practice session is no longer active.");
        }
        return session;
    }

    private record Progress(
        int spokenSeconds, int budgetSeconds, int targetBandOrder, int targetBandCount,
        UUID targetFrameworkVersionId) {
    }

    /**
     * Computed EARLY, even before the idempotent branch: every returned branch must carry the pair of numbers for the
     * progress bar, including the branch that returns the old question unchanged after a call that timed out.
     */
    private Progress measureProgress(PracticeSession session) {
        int spokenSeconds = session.getGradedSeconds();
        int targetBandOrder = enrichmentService.bandOrder(session.getTargetFrameworkBandId());
        int targetBandCount = enrichmentService.frameworkBandCountOfBand(session.getTargetFrameworkBandId());
        UUID targetFrameworkVersionId =
            enrichmentService.frameworkVersionIdOfBand(session.getTargetFrameworkBandId());
        int budgetSeconds = enrichmentService.sessionBudgetSeconds(
            session.getStudentId(), targetBandOrder
        );
        return new Progress(spokenSeconds, budgetSeconds, targetBandOrder, targetBandCount, targetFrameworkVersionId);
    }

    private Optional<Claim> findIdempotentClaim(PracticeSession session, Progress progress, List<UUID> alreadyChosenIds) {
        if (alreadyChosenIds.isEmpty()) {
            return Optional.empty();
        }
        UUID latestQuestionId = alreadyChosenIds.getLast();
        if (practiceItemResponseRepository.existsResponse(session.getId(), latestQuestionId)) {
            return Optional.empty();
        }
        PracticeQuestion latestQuestion = questionRepository.findById(latestQuestionId)
            .orElseThrow(() -> new ResourceNotFoundException("Practice question not found."));
        return Optional.of(claimOf(session, progress, null, null, null, latestQuestion, alreadyChosenIds.size(), null));
    }

    /**
     * Is there still room for a meaningful speaking turn? Measured in seconds ACTUALLY SPOKEN, not the planned
     * budget of the questions already issued.
     *
     * <p>The previous version compared sumPlannedSecondsForPaper (the total of max_response_seconds) with the ceiling.
     * On real data the plan is 40-70% higher than reality (60 vs 29, 45 vs 27), so a session
     * reported the budget as exhausted while nearly half the quota was left. And quota is charged by the REAL cost of the
     * turns spoken (SubmitPracticeTurnUseCase converts turnCostUsd to VND and hands it to
     * ConsumeQuotaService), i.e. proportional to the real speaking time and not to the planned budget --
     * two clocks measuring two different quantities will sooner or later drift apart.
     *
     * <p>This check comes BEFORE choosing/generating the question, and that is the main point: the previous version checked AFTER
     * resolveNextQuestion had run, so it could pay 10-40 seconds of LLM calls to generate a question and then throw it
     * away because the budget was exhausted.
     *
     * <p>Speaking past the threshold midway is fine: the quotaExhausted flag takes care of that -- the turn is still
     * recorded and graded, and the session closes properly after the result is returned.
     */
    private boolean isBudgetExhausted(Progress progress) {
        return progress.spokenSeconds() + MINIMUM_USEFUL_TURN_SECONDS > progress.budgetSeconds();
    }

    private Claim claimOf(
            PracticeSession session, Progress progress, PracticeTopic topic, PracticeFocusInfo focus,
            List<PracticeQuestion> alreadyChosen, PracticeQuestion idempotentQuestion, int idempotentSlot,
            String earlyExitReason) {
        return new Claim(
            session.getStudentId(), session.getPracticePaperId(), topic, focus, alreadyChosen,
            idempotentQuestion, idempotentSlot, earlyExitReason,
            progress.spokenSeconds(), progress.budgetSeconds(), progress.targetBandOrder(),
            progress.targetBandCount(), progress.targetFrameworkVersionId()
        );
    }

}
