package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PracticeFrameworkConfigPort;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.model.framework.FrameworkResultBand;

/**
 * Minutes and the duration cap for topic offers -- gathered in 1 place because there are many callers
 * (BuildPracticePaperUseCase, ViewPracticeTopicOffersUseCase, SearchPracticeTopicsUseCase,
 * PickRandomTopicUseCase, TopicSuggestionService).
 *
 * <p>NO LONGER computes the band for the student. This class used to infer the band from three sources (the band measured
 * from grading, the performance EMA, the most recent abandoned attempt per topic) -- i.e. it took the difficulty of the QUESTIONS
 * assigned and turned it into the proficiency of the LEARNER, a claim the system cannot prove. Now the student
 * chooses the band to practise each session, and that band only means "the difficulty I want today".
 */
@Service
public class PracticeTopicOfferEnrichmentService {

    /** Used when the student is not tied to any grading policy -- keeps the old behaviour for data
     * that is not configured, not because the system defaults to VSTEP. */
    private static final int DEFAULT_BAND_COUNT = 6;

    /** Only used when the session's band has been removed from the framework -- see {@link #bandOrder}. */
    private static final int DEFAULT_BAND_ORDER = 3;

    private final LearnerProfileRepository learnerProfileRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    /** Empty = automatically pick the active framework. Fill in a code to pin a specific framework. */
    private final String practiceFrameworkVersionCode;

    public PracticeTopicOfferEnrichmentService(
            LearnerProfileRepository learnerProfileRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            PracticeFrameworkConfigPort practiceFrameworkConfigPort) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        String practiceFrameworkVersionCode = practiceFrameworkConfigPort.frameworkVersionCode();
        this.practiceFrameworkVersionCode =
            practiceFrameworkVersionCode == null || practiceFrameworkVersionCode.isBlank()
                ? null
                : practiceFrameworkVersionCode.trim();
    }

    /**
     * The order of a band on the scale. A band that has been removed from the framework falls back to the middle of the scale -- better
     * to ask slightly off than to kill a running session because the configuration data changed behind its back.
     */
    public int bandOrder(UUID bandId) {
        if (bandId == null) {
            return DEFAULT_BAND_ORDER;
        }
        return frameworkResultBandRepository.findById(bandId)
            .map(band -> band.getOrder())
            .orElse(DEFAULT_BAND_ORDER);
    }

    /**
     * The number of bands of the framework THIS BAND belongs to -- paired with {@link #bandOrder(UUID)}.
     *
     * <p>Derived from the band itself rather than looking up the framework in force, the same way
     * {@code SpringDataPracticeSessionRepository.findCriteriaFrameworks} derives the framework at grading time.
     * So a session opened under one framework keeps that framework to the end, even if the admin
     * publishes another framework midway.
     */
    public int frameworkBandCountOfBand(UUID bandId) {
        if (bandId == null) {
            return DEFAULT_BAND_COUNT;
        }
        return frameworkResultBandRepository.findById(bandId)
            .map(band -> frameworkBandCount(band.getFrameworkVersionId()))
            .orElse(DEFAULT_BAND_COUNT);
    }

    /**
     * The framework this band belongs to. {@code null} when it cannot be looked up -- the caller falls back to the
     * framework in force, keeping the old behaviour instead of blocking the whole practice session.
     */
    public UUID frameworkVersionIdOfBand(UUID bandId) {
        if (bandId == null) {
            return null;
        }
        return frameworkResultBandRepository.findById(bandId)
            .map(band -> band.getFrameworkVersionId())
            .orElse(null);
    }

    /**
     * The number of bands of the competency scale. Defaults to {@value #DEFAULT_BAND_COUNT} when no framework is
     * in force -- not because of VSTEP, but because it is a safe value that keeps the old behaviour
     * for data that is not configured.
     *
     * <p>Since V13 it no longer takes studentId: the assessment framework for practice is a SYSTEM-WIDE concept,
     * no longer inferred from the student's class through an assessment policy.
     */
    public int frameworkBandCount() {
        return frameworkBandCount(practiceFrameworkVersionId());
    }

    /**
     * The number of bands of ONE specific framework -- used when the student has chosen a framework, instead of taking the
     * system-wide framework in force.
     *
     * <p>Needed since the practice screen lets the student pick a framework: two frameworks can differ in band count (6 for
     * KNLNNVN/CEFR, 9 for IELTS), and the band count goes straight into the difficulty clamp in
     * {@code PracticeQuestionSelectionService} and {@code TensePolicy}. Taking the wrong band count
     * maps difficulty off with no error at all.
     */
    public int frameworkBandCount(UUID frameworkVersionId) {
        List<Integer> values = learnerProfileRepository.findFrameworkBandCount(
            frameworkVersionId == null ? NO_FRAMEWORK : frameworkVersionId);
        if (values.isEmpty()) {
            return DEFAULT_BAND_COUNT;
        }
        Integer bandCount = values.get(0);
        return bandCount == null || bandCount < 1 ? DEFAULT_BAND_COUNT : bandCount;
    }

    /** The whole band scale with descriptions, to send to Python to build the ladder in the question-grading prompt.
     * Empty lets Python fall back to its own default ladder. */
    public List<FrameworkResultBand> frameworkBandLadder() {
        return frameworkBandLadder(practiceFrameworkVersionId());
    }

    /** The band scale of ONE specific framework. See {@link #frameworkBandCount(UUID)} for why. */
    public List<FrameworkResultBand> frameworkBandLadder(UUID frameworkVersionId) {
        return learnerProfileRepository.findFrameworkBandLadder(
            frameworkVersionId == null ? NO_FRAMEWORK : frameworkVersionId);
    }

    /** Every framework in force with its latest version -- the source for the framework picker before picking a band. */
    public List<FrameworkVersionRepository.ActiveFramework> activeFrameworks() {
        return frameworkVersionRepository.findActiveFrameworks();
    }

    /**
     * The assessment framework used for practice. When no version is in force it returns a UUID that does NOT
     * exist instead of throwing: both functions that call it have a sensible fallback (default band count,
     * empty ladder so Python falls back itself), so blocking the whole difficulty-selection screen over missing configuration is a reaction
     * heavier than the problem.
     */
    private UUID practiceFrameworkVersionId() {
        return frameworkVersionRepository.findActiveVersionId(practiceFrameworkVersionCode)
            .orElse(NO_FRAMEWORK);
    }

    private static final UUID NO_FRAMEWORK = new UUID(0L, 0L);

    /**
     * The length cap of ONE session by the band BEING PRACTISED (package design 6 section 4.1: band 1-2 → 720s,
     * band 3 → 900s, band 4+ → 1200s).
     *
     * Different in nature from the subscription quota ({@link #minutesForStudent}): that is a COMMERCIAL limit
     * (how much the school bought), this is a PEDAGOGICAL limit (how long is reasonable to practise
     * continuously at this band). BOTH must apply, taking the smaller -- with only the plan quota, a
     * student who has just started could still be pushed into a 20-minute session.
     *
     * <p>The parameter is the band the student CHOSE for this session, not a band the system guessed -- that is
     * the whole difference from the previous version.
     */
    public int sessionSecondsCapForBand(int bandOrder, int bandCount) {
        // Divide by the PROPORTIONAL position on the scale, no hard cut at band 2/3: with 6 bands it gives exactly
        // 720/720/900/1200/1200/1200 as before, and with another scale it stretches along.
        double ratio = (double) bandOrder / Math.max(1, bandCount);
        if (ratio <= 1.0 / 3) {
            return 720;
        }
        return ratio <= 1.0 / 2 ? 900 : 1200;
    }

    /**
     * The REAL speaking budget of a session: the narrower of the plan quota and the band cap.
     *
     * This formula used to be written directly in {@code ResolveNextPracticeQuestionClaimService}
     * -- the only place that needed it. Now the practice screen must also show "spoken / budget", so two
     * places need the same number; letting each recompute it opens the way for them to drift apart, and then the
     * progress bar on the student's device says one thing while the session stops at another.
     */
    public int sessionBudgetSeconds(UUID studentId, int bandOrder) {
        return Math.min(
            minutesForStudent(studentId) * 60,
            sessionSecondsCapForBand(bandOrder, frameworkBandCount())
        );
    }

    /** Minutes per practice turn according to the active subscription -- 0 if there is no plan. */
    public int minutesForStudent(UUID studentId) {
        Integer minutes = schoolSubscriptionRepository.findMaxTimePerAttemptMinForUser(studentId);
        return minutes == null ? 0 : minutes;
    }

}
