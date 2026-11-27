package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.MissingResponseBackfillService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
@RequiredArgsConstructor
public class SubmitAssessmentSessionUseCase implements IUseCase<SubmitAssessmentSessionCommand, Void> {

    private static final Set<String> LADDER_CRITERION_KEYS = Set.of("grammar", "vocabulary", "coherence");

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final MissingResponseBackfillService missingResponseBackfillService;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final ExternalEventPublisherPort externalEventPublisherPort;
    private final ZeroScoreAssessmentResultService zeroScoreAssessmentResultService;

    @Override
    @Transactional
    public Void execute(SubmitAssessmentSessionCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không thể tìm thấy phiên thi"));
        AssessmentSessionStatus fromStatus = requireGradableStatus(session);
        if (!assessmentSessionRepository.tryTransitionStatus(
                session.getId(), fromStatus, AssessmentSessionStatus.GRADING)) {
            return null;
        }
        session.setStatus(AssessmentSessionStatus.GRADING);
        AssessmentCandidate candidate = findCandidate(session);
        Assessment assessment = findAssessment(session);
        List<AssessmentItemResponse> responses = assessmentItemResponseRepository.findBySessionId(session.getId());
        if (candidate.getBlockedAt() != null) {
            persistInvalidBlockedResult(session);
            return null;
        }
        if (!AssessmentCandidateStatus.isAttended(candidate.getStatus()) || responses.isEmpty()) {
            releaseZeroAndMarkGraded(session);
            return null;
        }

        publishEvaluationRequests(session, assessment, responses);
        return null;
    }

    /**
     * GRADED is accepted here for exactly 1 case: G.4 - a session that was wrongly flagged as a
     * violation (INVALID, never had any AssessmentItemEvaluation), has been unblocked, and is called back by
     * RetryGradingAssessmentSessionUseCase for the AI to really grade it for the first time.
     */
    private AssessmentSessionStatus requireGradableStatus(AssessmentSession session) {
        AssessmentSessionStatus fromStatus = session.getStatus();
        if (fromStatus != AssessmentSessionStatus.SUBMITTED
                && fromStatus != AssessmentSessionStatus.EXPIRED
                && fromStatus != AssessmentSessionStatus.GRADING_FAILED
                && fromStatus != AssessmentSessionStatus.GRADED) {
            throw new IllegalArgumentException("chỉ được gửi chấm khi phiên thi đã nộp hoặc hết giờ");
        }
        return fromStatus;
    }

    private AssessmentCandidate findCandidate(AssessmentSession session) {
        return assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy thí sinh của phiên thi"));
    }

    private Assessment findAssessment(AssessmentSession session) {
        return assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy bài kiểm tra"));
    }

    /**
     * Applies to every kind of assessment: the exam entrance already blocks anyone who has not checked in, this is
     * the second line of defence.
     */
    private void releaseZeroAndMarkGraded(AssessmentSession session) {
        zeroScoreAssessmentResultService.releaseZeroForEmptySession(session.getId());
        session.setStatus(AssessmentSessionStatus.GRADED);
        assessmentSessionRepository.save(session);
    }

    /**
     * Fills the questions that have NO record (timeout, lost connection, forced end) with an empty response pair +
     * a 0-score evaluation. It runs AFTER the two early exits in execute because both already handle the whole
     * attempt themselves (releaseZeroForEmptySession), and BEFORE the AI event loop -- that loop runs on the
     * `responses` read earlier so the rows just created do not get in, which is intended: no empty transcript is
     * pushed to the LLM. See MissingResponseBackfillService.
     */
    private void publishEvaluationRequests(
        AssessmentSession session, Assessment assessment, List<AssessmentItemResponse> responses) {
        missingResponseBackfillService.backfill(session.getId(), session.getPaperId());

        List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> criteriaFrameworks =
            buildCriteriaFrameworks(assessment.getAssessmentPolicyId());

        for (AssessmentItemResponse response : responses) {
            if (MissingResponseBackfillService.NO_RECORDING.equals(response.getTerminationReason())) {
                continue;
            }
            UUID paperItemId = response.getPaperItemId();
            if (paperItemId == null) {
                throw new ResourceNotFoundException("không thể tìm thấy paperItemId cho câu trả lời " + response.getId());
            }
            if (recordedAsSilentAnswer(response, paperItemId)) {
                continue;
            }
            externalEventPublisherPort.publish(
                buildEvaluationRequestedEvent(session, response, paperItemId, criteriaFrameworks));
        }
    }

    /**
     * The candidate WAS asked but SAID NOTHING: write 0 directly, do not send it to the LLM.
     *
     * <p>This is the end of the silence chain -- the AI re-asks up to the cap (3 consecutive turns) and then cuts
     * the question, but the response row still exists with an empty transcript. Pushing it to the LLM invites the
     * model to grade an answer with no content: it receives the full question, an empty answer, and still returns
     * a score inferred from nothing -- indistinguishable from a real score.
     *
     * <p>The "silent" predicate lives in MissingResponseBackfillService -- see the javadoc there for why it has to
     * be one shared definition instead of each place checking by itself.
     */
    private boolean recordedAsSilentAnswer(AssessmentItemResponse response, UUID paperItemId) {
        if (!missingResponseBackfillService.isSilentAnswer(response)) {
            return false;
        }
        missingResponseBackfillService.recordSilentAnswer(response.getId(), paperItemId);
        return true;
    }

    private AssessmentAttemptEvaluationRequestedExternalEvent buildEvaluationRequestedEvent(
        AssessmentSession session,
        AssessmentItemResponse response,
        UUID paperItemId,
        List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> criteriaFrameworks) {
        AssessmentPaperItem paperItem = assessmentPaperItemRepository.findById(paperItemId)
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy paper item " + paperItemId));
        Question question = questionRepository.findById(paperItem.getQuestionId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy câu hỏi " + paperItem.getQuestionId()));
        QuestionEvaluationGuide guide = questionEvaluationGuideRepository.findByQuestionId(question.getId()).orElse(null);
        QuestionAsset asset = questionAssetRepository.findByQuestionId(question.getId()).stream().findFirst().orElse(null);
        QuestionTopic topic = question.getQuestionTopicId() == null
            ? null
            : questionTopicRepository.findById(question.getQuestionTopicId()).orElse(null);
        List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> turns = buildTurnInputs(response);

        return new AssessmentAttemptEvaluationRequestedExternalEvent(
            session.getId().toString(),
            response.getId().toString(),
            question.getId().toString(),
            buildEvaluationPayload(question, asset, topic, guide, criteriaFrameworks, turns)
        );
    }

    private List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> buildTurnInputs(
        AssessmentItemResponse response) {
        List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> turns = assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(response.getId()).stream()
            .sorted(Comparator.comparingInt(turn -> turn.getTurnOrder()))
            .map(turn -> new AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput(
                turn.getTurnOrder(),
                turn.getTurnType().name(),
                turn.getPromptText(),
                turn.getAudioUrl(),
                turn.getTranscript(),
                turn.getDurationSeconds()
            ))
            .toList();
        if (turns.isEmpty()) {
            turns = List.of(new AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput(
                1,
                "MAIN",
                null,
                response.getAudioUrl(),
                response.getTranscript(),
                response.getDurationSeconds()
            ));
        }
        return turns;
    }

    private AssessmentAttemptEvaluationRequestedExternalEvent.Payload buildEvaluationPayload(
        Question question,
        QuestionAsset asset,
        QuestionTopic topic,
        QuestionEvaluationGuide guide,
        List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> criteriaFrameworks,
        List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> turns) {
        return new AssessmentAttemptEvaluationRequestedExternalEvent.Payload(
            question.getQuestionText(),
            question.getType() == null ? null : question.getType().name(),
            null,
            question.getMaxResponseSeconds(),
            question.getMinResponseSeconds(),
            question.getMaxResponseSeconds(),
            toEventAsset(asset),
            topic == null ? null : topic.getName(),
            topic == null ? null : topic.getDescription(),
            toEventEvaluationGuide(guide),
            "unscripted",
            null,
            "en-US",
            criteriaFrameworks,
            turns
        );
    }

    private AssessmentAttemptEvaluationRequestedExternalEvent.Asset toEventAsset(QuestionAsset asset) {
        return asset == null ? null : new AssessmentAttemptEvaluationRequestedExternalEvent.Asset(
            asset.getType() == null ? null : asset.getType().name(),
            asset.getTranscript(),
            asset.getDescription(),
            asset.getAltText()
        );
    }

    private AssessmentAttemptEvaluationRequestedExternalEvent.EvaluationGuide toEventEvaluationGuide(
        QuestionEvaluationGuide guide) {
        return guide == null ? null : new AssessmentAttemptEvaluationRequestedExternalEvent.EvaluationGuide(
            guide.getExpectedContent(),
            guide.getKeyPoints(),
            guide.getAcceptableResponses(),
            guide.getOffTopicExamples(),
            guide.getScoringHints(),
            guide.getCommonMistakes()
        );
    }

    /**
     * Whether a question has any answer content.
     *
     * <p>It considers BOTH places a transcript can live: the total field of the response, and the transcript of each
     * speaking turn. The two write paths fill two different places, so looking at only one would wrongly conclude
     * "empty" and write 0 for an attempt the candidate did -- wrong in the most dangerous direction.
     *
     * <p>It does NOT consider duration: an open mic with a silent candidate still yields dozens of seconds of audio
     * with no words. Only words can be graded.
     */
    private void persistInvalidBlockedResult(AssessmentSession session) {
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy bài kiểm tra"));
        AssessmentPolicy policy = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId()).orElse(null);
        AssessmentCandidateResult existing = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        AssessmentCandidateResult result = existing == null ? new AssessmentCandidateResult() : existing;
        Instant now = Instant.now();

        fillInvalidBlockedResult(result, session, policy, now);
        assessmentCandidateResultRepository.save(result);

        session.setStatus(AssessmentSessionStatus.GRADED);
        assessmentSessionRepository.save(session);
    }

    private void fillInvalidBlockedResult(
        AssessmentCandidateResult result, AssessmentSession session, AssessmentPolicy policy, Instant now) {
        result.setAssessmentId(session.getAssessmentId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy == null ? null : policy.getId());
        result.setPolicyVersion(policy == null ? 0 : policy.getVersion());
        result.setRubricVersionId(policy == null ? null : policy.getRubricVersionId());
        result.setFrameworkVersionId(policy == null ? null : policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy == null ? null : policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(null);
        result.setTotalScore(null);
        result.setStatus(AssessmentCandidateResultStatus.INVALID);
        result.setFinalizedAt(now);
    }

    private List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> buildCriteriaFrameworks(UUID assessmentPolicyId) {
        if (assessmentPolicyId == null) {
            return List.of();
        }

        AssessmentPolicy policy = assessmentPolicyRepository.findById(assessmentPolicyId).orElse(null);
        if (policy == null) {
            return List.of();
        }
        FrameworkResultBand targetBand = requireTargetBand(policy);

        List<RubricCriterion> rubricCriteria = rubricCriterionRepository.findByRubricVersionId(policy.getRubricVersionId());
        if (rubricCriteria.isEmpty()) {
            return List.of();
        }

        CriterionFrameworkLookups lookups = loadCriterionFrameworkLookups(rubricCriteria);
        return rubricCriteria.stream()
            .map(criterion -> toCriterionFramework(criterion, policy, targetBand, lookups))
            .toList();
    }

    private FrameworkResultBand requireTargetBand(AssessmentPolicy policy) {
        if (policy.getTargetFrameworkBandId() == null) {
            throw new IllegalStateException("Assessment policy chưa cấu hình bậc mục tiêu để AI chấm.");
        }
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(policy.getTargetFrameworkBandId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bậc mục tiêu của assessment policy."));
        if (!policy.getFrameworkVersionId().equals(targetBand.getFrameworkVersionId())) {
            throw new IllegalStateException("Bậc mục tiêu không thuộc framework version của assessment policy.");
        }
        return targetBand;
    }

    private CriterionFrameworkLookups loadCriterionFrameworkLookups(List<RubricCriterion> rubricCriteria) {
        List<UUID> frameworkCriterionIds = rubricCriteria.stream()
            .map(item -> item.getFrameworkCriterionId())
            .filter(id -> id != null)
            .toList();
        Map<UUID, FrameworkCriterion> frameworkCriteriaById = frameworkCriterionRepository.findAllByIds(frameworkCriterionIds).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity()));
        List<FrameworkCriterionBand> frameworkCriterionBands = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(frameworkCriterionIds);
        Map<UUID, List<FrameworkCriterionBand>> frameworkBandsByCriterionId = frameworkCriterionBands.stream()
            .collect(Collectors.groupingBy(item -> item.getFrameworkCriterionId()));
        List<UUID> frameworkResultBandIds = frameworkCriterionBands.stream()
            .map(item -> item.getFrameworkResultBandId())
            .distinct()
            .toList();
        Map<UUID, FrameworkResultBand> frameworkResultBandsById = frameworkResultBandRepository.findAllByIds(frameworkResultBandIds).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity()));
        return new CriterionFrameworkLookups(frameworkCriteriaById, frameworkBandsByCriterionId, frameworkResultBandsById);
    }

    private AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework toCriterionFramework(
        RubricCriterion criterion,
        AssessmentPolicy policy,
        FrameworkResultBand targetBand,
        CriterionFrameworkLookups lookups) {
        requireValidScoreRange(criterion);
        FrameworkCriterion frameworkCriterion = lookups.criteriaById().get(criterion.getFrameworkCriterionId());
        List<FrameworkCriterionBand> criterionBands = lookups.bandsByCriterionId()
            .getOrDefault(criterion.getFrameworkCriterionId(), List.of());
        FrameworkCriterionBand targetCriterionBand = findTargetCriterionBand(criterionBands, policy, targetBand);
        String criterionKey = agentCriterionKey(criterion.getCode());
        List<FrameworkCriterionBand> sourceBands = criterionKey != null && LADDER_CRITERION_KEYS.contains(criterionKey)
            ? criterionBands
            : List.of(targetCriterionBand);
        List<AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand> bands = sourceBands.stream()
            .map(item -> toFrameworkBand(item, criterion, lookups))
            .sorted(Comparator.comparing(
                band -> band.order()
            ))
            .toList();

        return assembleCriterionFramework(criterionKey, frameworkCriterion, policy, targetBand, criterion, bands);
    }

    private void requireValidScoreRange(RubricCriterion criterion) {
        if (criterion.getMinScore() == null || criterion.getMaxScore() == null
                || criterion.getMinScore().compareTo(criterion.getMaxScore()) >= 0) {
            throw new IllegalArgumentException("Tiêu chí rubric '" + criterion.getName()
                + "' phải có minScore nhỏ hơn maxScore trước khi gửi sang AI.");
        }
    }

    private FrameworkCriterionBand findTargetCriterionBand(
        List<FrameworkCriterionBand> criterionBands, AssessmentPolicy policy, FrameworkResultBand targetBand) {
        return criterionBands.stream()
            .filter(item -> policy.getTargetFrameworkBandId().equals(item.getFrameworkResultBandId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Tiêu chí framework chưa có mô tả cho bậc mục tiêu " + targetBand.getLabel() + "."));
    }

    private AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand toFrameworkBand(
        FrameworkCriterionBand item, RubricCriterion criterion, CriterionFrameworkLookups lookups) {
        FrameworkResultBand resultBand = lookups.resultBandsById().get(item.getFrameworkResultBandId());
        if (resultBand == null) {
            throw new IllegalStateException(
                "Không tìm thấy bậc framework cho tiêu chí '" + criterion.getName() + "'.");
        }
        return new AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand(
            resultBand.getCode(),
            resultBand.getLabel(),
            criterion.getMinScore().doubleValue(),
            criterion.getMaxScore().doubleValue(),
            item.getDescriptor(),
            signalDescriptions(item.getPositiveSignals()),
            signalDescriptions(item.getNegativeSignals()),
            resultBand.getOrder()
        );
    }

    private List<String> signalDescriptions(FrameworkCriterionSignals signals) {
        return signals == null
            ? List.of()
            : signals.values().stream()
                .map(signal -> signal.description())
                .toList();
    }

    private AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework assembleCriterionFramework(
        String criterionKey,
        FrameworkCriterion frameworkCriterion,
        AssessmentPolicy policy,
        FrameworkResultBand targetBand,
        RubricCriterion criterion,
        List<AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand> bands) {
        return new AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework(
            criterionKey,
            frameworkCriterion == null ? null : frameworkCriterion.getCode(),
            frameworkCriterion == null ? null : frameworkCriterion.getName(),
            frameworkCriterion == null ? null : frameworkCriterion.getDescription(),
            policy.getTargetFrameworkBandId().toString(),
            targetBand.getCode(),
            targetBand.getLabel(),
            true,
            criterion.getWeight() == null ? null : criterion.getWeight().doubleValue(),
            criterion.getMinScore().doubleValue(),
            criterion.getMaxScore().doubleValue(),
            bands
        );
    }

    private String agentCriterionKey(String rubricCriterionCode) {
        if (rubricCriterionCode == null) {
            return null;
        }
        String normalized = rubricCriterionCode.trim().toLowerCase(Locale.ROOT);
        return "discourse".equals(normalized) ? "coherence" : normalized;
    }

    private record CriterionFrameworkLookups(
        Map<UUID, FrameworkCriterion> criteriaById,
        Map<UUID, List<FrameworkCriterionBand>> bandsByCriterionId,
        Map<UUID, FrameworkResultBand> resultBandsById) {
    }
}
