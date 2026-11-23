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
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitAssessmentSessionCommand;
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
            .orElseThrow(() -> new NotFoundException("Không thể tìm thấy phiên thi"));
        // GRADED is accepted here for exactly 1 case: G.4 - a session that was wrongly flagged as a
        // violation (INVALID, never had any AssessmentItemEvaluation), has been unblocked, and is called back by
        // RetryGradingAssessmentSessionUseCase for the AI to really grade it for the first time.
        AssessmentSessionStatus fromStatus = session.getStatus();
        if (fromStatus != AssessmentSessionStatus.SUBMITTED
                && fromStatus != AssessmentSessionStatus.EXPIRED
                && fromStatus != AssessmentSessionStatus.GRADING_FAILED
                && fromStatus != AssessmentSessionStatus.GRADED) {
            throw new IllegalStateException("chỉ được gửi chấm khi phiên thi đã nộp hoặc hết giờ");
        }

       
        boolean claimed = assessmentSessionRepository.tryTransitionStatus(
            session.getId(), fromStatus, AssessmentSessionStatus.GRADING);
        if (!claimed) {
            return null;
        }
        session.setStatus(AssessmentSessionStatus.GRADING);

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy thí sinh của phiên thi"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy bài kiểm tra"));
        List<AssessmentItemResponse> responses = assessmentItemResponseRepository.findBySessionId(session.getId());
        if (candidate.getBlockedAt() != null) {
            persistInvalidBlockedResult(session);
            return null;
        }
        // Áp dụng cho mọi loại bài: cổng vào thi đã chặn người chưa điểm danh, đây là lớp chốt thứ hai.
        if (!AssessmentCandidateStatus.isAttended(candidate.getStatus())) {
            zeroScoreAssessmentResultService.releaseZeroForEmptySession(session.getId());
            session.setStatus(AssessmentSessionStatus.GRADED);
            assessmentSessionRepository.save(session);
            return null;
        }
        if (responses.isEmpty()) {
            zeroScoreAssessmentResultService.releaseZeroForEmptySession(session.getId());
            session.setStatus(AssessmentSessionStatus.GRADED);
            assessmentSessionRepository.save(session);
            return null;
        }

        // Lấp câu KHÔNG có bản ghi (hết giờ, mất kết nối, buộc kết thúc) bằng cặp response rỗng
        // + bản chấm 0 điểm. Đặt SAU hai nhánh thoát sớm ở trên vì cả hai đã tự xử trọn bài
        // (releaseZeroForEmptySession), và TRƯỚC vòng bắn sự kiện AI bên dưới -- vòng đó chạy
        // trên `responses` đọc lúc nãy nên các dòng vừa tạo không lọt vào, đúng ý: không đẩy
        // transcript rỗng sang LLM. Xem MissingResponseBackfillService.
        missingResponseBackfillService.backfill(session.getId(), session.getPaperId());

        List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> criteriaFrameworks = buildCriteriaFrameworks(assessment.getAssessmentPolicyId());

        for (AssessmentItemResponse response : responses) {
            if (MissingResponseBackfillService.NO_RECORDING.equals(response.getTerminationReason())) {
                continue;
            }
            UUID paperItemId = response.getPaperItemId();
            if (paperItemId == null) {
                throw new NotFoundException("không thể tìm thấy paperItemId cho câu trả lời " + response.getId());
            }

            // Thí sinh ĐƯỢC hỏi nhưng KHÔNG NÓI GÌ: ghi thẳng 0 điểm, không gửi sang LLM.
            //
            // Đây là kết cục của chuỗi im lặng -- AI hỏi lại tới trần (3 lượt liên tiếp) rồi cắt
            // câu, nhưng dòng response vẫn tồn tại với transcript rỗng. Đẩy nó sang LLM là mời
            // model chấm một bài không có nội dung: nó nhận đề bài đầy đủ, phần trả lời trống,
            // và vẫn trả về một con điểm suy ra từ hư không -- nhìn không phân biệt được với
            // điểm thật.
            //
            // Vị từ "im lặng" nằm ở MissingResponseBackfillService -- xem javadoc ở đó để biết
            // vì sao nó phải là một định nghĩa dùng chung thay vì mỗi nơi tự kiểm.
            if (missingResponseBackfillService.isSilentAnswer(response)) {
                missingResponseBackfillService.recordSilentAnswer(response.getId(), paperItemId);
                continue;
            }

            AssessmentPaperItem paperItem = assessmentPaperItemRepository.findById(paperItemId)
                .orElseThrow(() -> new NotFoundException("không thể tìm thấy paper item " + paperItemId));
            Question question = questionRepository.findById(paperItem.getQuestionId())
                .orElseThrow(() -> new NotFoundException("không thể tìm thấy câu hỏi " + paperItem.getQuestionId()));
            QuestionEvaluationGuide guide = questionEvaluationGuideRepository.findByQuestionId(question.getId()).orElse(null);
            QuestionAsset asset = questionAssetRepository.findByQuestionId(question.getId()).stream().findFirst().orElse(null);
            QuestionTopic topic = question.getQuestionTopicId() == null
                ? null
                : questionTopicRepository.findById(question.getQuestionTopicId()).orElse(null);
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

            AssessmentAttemptEvaluationRequestedExternalEvent event = new AssessmentAttemptEvaluationRequestedExternalEvent(
                session.getId().toString(),
                response.getId().toString(),
                question.getId().toString(),
                new AssessmentAttemptEvaluationRequestedExternalEvent.Payload(
                    question.getQuestionText(),
                    question.getType() == null ? null : question.getType().name(),
                    null,
                    question.getMaxResponseSeconds(),
                    question.getMinResponseSeconds(),
                    question.getMaxResponseSeconds(),
                    asset == null ? null : new AssessmentAttemptEvaluationRequestedExternalEvent.Asset(
                        asset.getType() == null ? null : asset.getType().name(),
                        asset.getTranscript(),
                        asset.getDescription(),
                        asset.getAltText()
                    ),
                    topic == null ? null : topic.getName(),
                    topic == null ? null : topic.getDescription(),
                    guide == null ? null : new AssessmentAttemptEvaluationRequestedExternalEvent.EvaluationGuide(
                        guide.getExpectedContent(),
                        guide.getKeyPoints(),
                        guide.getAcceptableResponses(),
                        guide.getOffTopicExamples(),
                        guide.getScoringHints(),
                        guide.getCommonMistakes()
                    ),
                    "unscripted",
                    null,
                    "en-US",
                    criteriaFrameworks,
                    turns
                )
            );
            externalEventPublisherPort.publish(event);
        }

        return null;
    }

    /**
     * Câu này có nội dung trả lời nào không.
     *
     * <p>Xét CẢ HAI nơi transcript có thể nằm: ô tổng của response, và transcript của từng lượt
     * nói. Hai đường ghi khác nhau điền hai chỗ khác nhau, nên chỉ nhìn một chỗ sẽ kết luận nhầm
     * là rỗng và ghi 0 cho một bài thí sinh đã làm -- sai theo hướng nguy hiểm nhất.
     *
     * <p>KHÔNG xét thời lượng: mic mở nhưng thí sinh im lặng vẫn cho ra vài chục giây audio mà
     * không có chữ nào. Chỉ có chữ mới chấm được.
     */
    private void persistInvalidBlockedResult(com.sep.vox.domain.model.assessment.AssessmentSession session) {
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy bài kiểm tra"));
        AssessmentPolicy policy = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId()).orElse(null);
        AssessmentCandidateResult existing = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        AssessmentCandidateResult result = existing == null ? new AssessmentCandidateResult() : existing;
        Instant now = Instant.now();

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
        assessmentCandidateResultRepository.save(result);

        session.setStatus(AssessmentSessionStatus.GRADED);
        assessmentSessionRepository.save(session);
    }

    private List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> buildCriteriaFrameworks(UUID assessmentPolicyId) {
        if (assessmentPolicyId == null) {
            return List.of();
        }

        AssessmentPolicy policy = assessmentPolicyRepository.findById(assessmentPolicyId).orElse(null);
        if (policy == null) {
            return List.of();
        }
        if (policy.getTargetFrameworkBandId() == null) {
            throw new IllegalStateException("Assessment policy chưa cấu hình bậc mục tiêu để AI chấm.");
        }
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(policy.getTargetFrameworkBandId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bậc mục tiêu của assessment policy."));
        if (!policy.getFrameworkVersionId().equals(targetBand.getFrameworkVersionId())) {
            throw new IllegalStateException("Bậc mục tiêu không thuộc framework version của assessment policy.");
        }

        List<RubricCriterion> rubricCriteria = rubricCriterionRepository.findByRubricVersionId(policy.getRubricVersionId());
        if (rubricCriteria.isEmpty()) {
            return List.of();
        }

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
        Set<String> ladderCriterionKeys = Set.of("grammar", "vocabulary", "coherence");

        return rubricCriteria.stream().map(criterion -> {
            if (criterion.getMinScore() == null || criterion.getMaxScore() == null
                    || criterion.getMinScore().compareTo(criterion.getMaxScore()) >= 0) {
                throw new IllegalStateException("Tiêu chí rubric '" + criterion.getName()
                    + "' phải có minScore nhỏ hơn maxScore trước khi gửi sang AI.");
            }
            FrameworkCriterion frameworkCriterion = frameworkCriteriaById.get(criterion.getFrameworkCriterionId());
            List<FrameworkCriterionBand> criterionBands = frameworkBandsByCriterionId
                .getOrDefault(criterion.getFrameworkCriterionId(), List.of());
            FrameworkCriterionBand targetCriterionBand = criterionBands.stream()
                .filter(item -> policy.getTargetFrameworkBandId().equals(item.getFrameworkResultBandId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Tiêu chí framework chưa có mô tả cho bậc mục tiêu " + targetBand.getLabel() + "."));
            String criterionKey = agentCriterionKey(criterion.getCode());
            List<FrameworkCriterionBand> sourceBands = criterionKey != null && ladderCriterionKeys.contains(criterionKey)
                ? criterionBands
                : List.of(targetCriterionBand);
            List<AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand> bands = sourceBands.stream()
                .map(item -> {
                    FrameworkResultBand resultBand = frameworkResultBandsById.get(item.getFrameworkResultBandId());
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
                        item.getPositiveSignals() == null
                            ? List.of()
                            : item.getPositiveSignals().values().stream()
                                .map(signal -> signal.description())
                                .toList(),
                        item.getNegativeSignals() == null
                            ? List.of()
                            : item.getNegativeSignals().values().stream()
                                .map(signal -> signal.description())
                                .toList(),
                        resultBand.getOrder()
                    );
                })
                .sorted(Comparator.comparing(
                    band -> band.order()
                ))
                .toList();

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
        }).toList();
    }

    private String agentCriterionKey(String rubricCriterionCode) {
        if (rubricCriterionCode == null) {
            return null;
        }
        String normalized = rubricCriterionCode.trim().toLowerCase(Locale.ROOT);
        return "discourse".equals(normalized) ? "coherence" : normalized;
    }
}
