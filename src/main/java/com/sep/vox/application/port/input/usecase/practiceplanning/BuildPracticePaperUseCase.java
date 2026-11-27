package com.sep.vox.application.port.input.usecase.practiceplanning;

import com.sep.vox.application.mapper.practiceplanning.PracticePaperDtoMapper;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.practiceplanning.PracticePlanningResponseMapper;
import com.sep.vox.application.port.input.command.practiceplanning.BuildPracticePaperCommand;
import com.sep.vox.application.port.input.service.PracticePaperPersistenceService;
import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionActiveGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Builds the practice paper -- ONLY the first MAIN question (as in README section 4.1: entering the session with
 * one suitable question is enough, no need to wait for the whole paper to be built). The following MAIN questions are
 * resolved while the session runs, see ResolveNextPracticeQuestionUseCase.
 */
@Service
@RequiredArgsConstructor
public class BuildPracticePaperUseCase implements IUseCase<BuildPracticePaperCommand, PracticePaper> {

    // Same scale as school_subscription_quota_records.used_amount_vnd numeric(18,6) -- the same constant
    // as in SubmitPracticeTurnUseCase, where the real cost of each turn is converted and deducted.
    private static final int COST_VND_SCALE = 6;

    private final PracticeTopicRepository topicRepository;
    private final PracticePaperRepository paperRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final PracticeQuestionSelectionService selectionService;
    private final PracticePaperPersistenceService persistenceService;
    private final UserContextPort userContextPort;
    private final ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase;
    private final QuotaPricingPort quotaPricingPort;
    private final SchoolSubscriptionActiveGuardService schoolSubscriptionActiveGuardService;
    private final PracticePaperDtoMapper practicePaperDtoMapper;

    /**
     * Deliberately NOT wrapped in a Spring transaction: resolveNextQuestion can call out
     * to the Python agents service (diversity check, and -- on a thin/new topic -- live
     * LLM question generation, which alone can take 10+ seconds). Holding a HikariCP
     * connection open for that whole external call starves the pool under load; confirmed
     * via HikariCP's own leak-detector firing on this exact method. Each individual
     * repository call still gets its own short-lived transaction from Spring Data's
     * repository proxy, so no atomicity is lost here -- only the final paper/item/exposure
     * write (persistenceService.persist, a separate bean so @Transactional actually applies
     * -- self-invocation within this class would bypass the AOP proxy) needs a real
     * transaction, scoped separately there.
     */
    @Override
    public PracticePaper execute(BuildPracticePaperCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        schoolSubscriptionActiveGuardService.requireActiveForStudent(studentId, "start a personalized AI practice session");
        PracticeTopic topic = requireTopic(input.topicId());
        BigDecimal spendableFundsVnd = spendablePracticeFundsVnd(studentId);
        PracticeFocusInfo focus = selectionService.resolveFocus(studentId, input.fromSubAttribute());
        ChosenBand chosen = chosenBand(input.targetFrameworkBandId());
        PracticeQuestionSelectionService.NextQuestionSelection selection = selectionService
            .resolveNextQuestion(topic, studentId, focus, chosen.order(), chosen.bandCount(),
                chosen.frameworkVersionId(), List.of())
            .orElseThrow(() -> new ResourceNotFoundException("The topic has no suitable practice question yet."));
        PracticeQuestion question = selection.question();
        requireFundsCoverWorstCaseCost(studentId, spendableFundsVnd, question);
        return persistAndMapToResponse(studentId, input, question, selection, chosen.order());
    }

    private void requireFundsCoverWorstCaseCost(UUID studentId, BigDecimal spendableFundsVnd,
                                                PracticeQuestion question) {
        // Worst-case estimate (seconds x calibrated price/second) -- not the real cost, in the same
        // spirit as Formula 1 of ClassTestTokenQuotaGuardService (see AI_USAGE_QUOTA_USD_MIGRATION.md
        // sections 5/6), but using the SEPARATE calibrated rate for PRACTICE (currentEstimatedCostPerPracticeSecondUsd,
        // see QuotaPricingCalibrationService/QuotaPricingSource) because the AI pipeline is very different from assessments. It also adds
        // reservedSeconds (questions awaiting submission from other sessions) so two sessions at the same time cannot
        // both exceed the quota while no session has submitted a real turn yet.
        //
        // Converted to VND right here: the listed price/second is in USD but both the quota wallet and the self-topped-up wallet are
        // VND columns, and this is exactly the number ConsumeQuotaService will really deduct at each spoken turn. It uses the COST PRICE,
        // does NOT add the service fee -- the fee was already charged once when the school placed the order, see ServiceFeePort.
        int reservedSeconds = paperRepository.sumReservedQuotaSeconds(studentId);
        BigDecimal estimatedCostVnd = BigDecimal.valueOf((long) reservedSeconds + question.spokenSeconds())
            .multiply(quotaPricingPort.currentEstimatedCostPerPracticeSecondUsd())
            .multiply(quotaPricingPort.usdToVndRate())
            .setScale(COST_VND_SCALE, RoundingMode.HALF_UP);
        if (spendableFundsVnd.compareTo(estimatedCostVnd) < 0) {
            throw new BusinessRuleViolationException("There is not enough quota or balance left for one complete practice question.");
        }
    }

    private PracticePaper persistAndMapToResponse(UUID studentId, BuildPracticePaperCommand input,
                                                  PracticeQuestion question,
                                                  PracticeQuestionSelectionService.NextQuestionSelection selection,
                                                  int chosenBandOrder) {
        com.sep.vox.domain.model.personalization.PracticePaper paper = persistenceService.persist(
            studentId,
            input.topicId(),
            input.targetFrameworkBandId(),
            resolveOrigin(studentId, input),
            input.offeredTopicIds(),
            input.previousOfferedTopicIds(),
            question,
            selection
        );
        return PracticePlanningResponseMapper.toResponse(
            practicePaperDtoMapper.toDto(
                paper,
                List.of(question),
                enrichmentService.sessionBudgetSeconds(studentId, chosenBandOrder)
            )
        );
    }

    /**
     * The real entry point of the session. The client only knows "the student tapped a card" so it sends SELECTED,
     * but that card may be an ε-greedy slot that the SYSTEM swapped into the offered batch. Recording SELECTED for it
     * breaks two things:
     *
     * <ul>
     *   <li>the interest signal becomes a false positive -- 0.95 as if the student chose it, when the topic was
     *       brought by the system. Since 2026-08-06 EPSILON has its own case in
     *       {@code InterestVectorService.recordSessionOutcome} and gets 0.60, in the same group as EXPLORATION;
     *       before that it fell into {@code default}, so even after being identified it still counted 0.95, meaning
     *       this half of the old comment described something that was not yet true.</li>
     *   <li>the exploration valve §2.7 never closes, because no vote carries the origin EPSILON to count.</li>
     * </ul>
     *
     * Inferred by re-ranking instead of remembering which slot was offered to whom: see
     * ViewPracticeTopicOffersUseCase.isOutsideTopRanked for why a cache is not used.
     */
    private String resolveOrigin(UUID studentId, BuildPracticePaperCommand input) {
        String origin = input.origin() == null ? "SELECTED" : input.origin();
        boolean cameFromOfferBatch = input.offeredTopicIds() != null
            && input.offeredTopicIds().contains(input.topicId());
        if (!"SELECTED".equals(origin) || !cameFromOfferBatch) {
            return origin;
        }
        return viewPracticeTopicOffersUseCase.isOutsideTopRanked(studentId, input.topicId())
            ? "EPSILON"
            : origin;
    }

    /**
     * Money still spendable for this practice turn = LEAST(school quota remaining + self-funded school balance,
     * personal quota remaining) -- see SchoolSubscriptionRepository.findPracticeSpendableFundsVnd.
     *
     * <p>Clamped to 0 because that number can be negative (a personal quota reduced below what was already used, or
     * old data where used exceeds allocated): "nothing left" must always be exactly one number, otherwise every
     * comparison below would have to remember to handle negatives itself.
     */
    private BigDecimal spendablePracticeFundsVnd(UUID studentId) {
        return schoolSubscriptionRepository.findPracticeSpendableFundsVnd(studentId).max(BigDecimal.ZERO);
    }

    /**
     * The order of the band the student chose, after checking that this band really exists in the framework in force
     * for them.
     *
     * <p>It must be checked, the id sent by the client must not be trusted directly: accepting a band of another
     * framework would make its {@code result_band_order} meaningless on this ladder, and every difficulty
     * comparison afterwards would silently be off. The band ladder already loaded for the picker screen is reused,
     * so no new query is needed.
     */
    /**
     * The band the student chose, validated against THE FRAMEWORK OF THAT BAND ITSELF.
     *
     * <p>It used to be matched against {@code frameworkBandLadder()} -- the framework in force system-wide. Since the
     * practice screen lets the student choose a framework, that rejected every band of a different framework, even
     * when that framework is still published and shown right on the screen.
     *
     * <p>It must still be validated rather than trusting the client: the band sent up may belong to a draft or to a
     * version that is no longer in force. The "still in force" condition comes from the same framework list that the
     * framework picker uses, so the two cannot drift apart.
     *
     * <p>It also returns the number of bands of that framework: {@code PracticeQuestionSelectionService} needs it to
     * clamp the difficulty, and taking it from the framework in force instead of the chosen one would map wrongly when
     * the two frameworks have different numbers of bands.
     */
    private ChosenBand chosenBand(UUID bandId) {
        if (bandId == null) {
            throw new ResourceNotFoundException("No band was chosen to practise.");
        }
        for (FrameworkVersionRepository.ActiveFramework framework : enrichmentService.activeFrameworks()) {
            Optional<FrameworkResultBand> match = enrichmentService.frameworkBandLadder(framework.versionId()).stream()
                .filter(band -> bandId.equals(band.getId()))
                .findFirst();
            if (match.isPresent()) {
                return new ChosenBand(
                    match.get().getOrder(),
                    enrichmentService.frameworkBandCount(framework.versionId()),
                    framework.versionId()
                );
            }
        }
        throw new ResourceNotFoundException("The practice band does not belong to any published framework.");
    }

    private record ChosenBand(int order, int bandCount, UUID frameworkVersionId) {
    }

    private PracticeTopic requireTopic(UUID topicId) {
        return topicRepository.findTopicById(topicId)
            .filter(candidate -> candidate.isActive())
            .orElseThrow(() -> new ResourceNotFoundException("Practice topic not found."));
    }
}
