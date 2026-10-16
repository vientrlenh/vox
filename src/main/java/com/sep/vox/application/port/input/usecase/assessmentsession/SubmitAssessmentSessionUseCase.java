package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.exception.ResourceNotFoundException;
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
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class SubmitAssessmentSessionUseCase implements IUseCase<SubmitAssessmentSessionCommand, Void> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;
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

    public SubmitAssessmentSessionUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentItemAnswerRepository assessmentItemAnswerRepository,
            MissingResponseBackfillService missingResponseBackfillService,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionTopicRepository questionTopicRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricCriterionRepository rubricCriterionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            ExternalEventPublisherPort externalEventPublisherPort,
            ZeroScoreAssessmentResultService zeroScoreAssessmentResultService) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
        this.missingResponseBackfillService = missingResponseBackfillService;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.externalEventPublisherPort = externalEventPublisherPort;
        this.zeroScoreAssessmentResultService = zeroScoreAssessmentResultService;
    }

    @Override
    @Transactional
    public Void execute(SubmitAssessmentSessionCommand input) {
        var session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không thể tìm thấy phiên thi"));
        // GRADED chỉ được chấp nhận ở đây cho đúng 1 trường hợp: G.4 - session từng bị đánh
        // dấu vi phạm oan (INVALID, chưa từng có AssessmentItemEvaluation), đã dỡ cấm, được
        // RetryGradingAssessmentSessionUseCase gọi lại để AI chấm thật lần đầu.
        var fromStatus = session.getStatus();
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

        var candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy thí sinh của phiên thi"));
        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy bài kiểm tra"));
        var responses = assessmentItemAnswerRepository.findBySessionId(session.getId());
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

        var criteriaFrameworks = buildCriteriaFrameworks(assessment.getAssessmentPolicyId());

        for (var response : responses) {
            if (MissingResponseBackfillService.NO_RECORDING.equals(response.getTerminationReason())) {
                continue;
            }
            var paperItemId = response.getPaperItemId();
            if (paperItemId == null) {
                throw new ResourceNotFoundException("không thể tìm thấy paperItemId cho câu trả lời " + response.getId());
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

            var paperItem = assessmentPaperItemRepository.findById(paperItemId)
                .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy paper item " + paperItemId));
            var question = questionRepository.findById(paperItem.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy câu hỏi " + paperItem.getQuestionId()));
            var guide = questionEvaluationGuideRepository.findByQuestionId(question.getId()).orElse(null);
            var asset = questionAssetRepository.findByQuestionId(question.getId()).stream().findFirst().orElse(null);
            var topic = question.getQuestionTopicId() == null
                ? null
                : questionTopicRepository.findById(question.getQuestionTopicId()).orElse(null);
            var turns = assessmentItemResponseTurnRepository.findByAssessmentItemAnswerId(response.getId()).stream()
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

            var event = new AssessmentAttemptEvaluationRequestedExternalEvent(
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
        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy bài kiểm tra"));
        var policy = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId()).orElse(null);
        var existing = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        var result = existing == null ? new AssessmentCandidateResult() : existing;
        var now = Instant.now();

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
        if (existing == null) {
            result.setCreatedAt(now);
            result.setCreatedBy(null);
        }
        result.setUpdatedAt(now);
        result.setUpdatedBy(null);
        assessmentCandidateResultRepository.save(result);

        session.setStatus(AssessmentSessionStatus.GRADED);
        assessmentSessionRepository.save(session);
    }

    private List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> buildCriteriaFrameworks(UUID assessmentPolicyId) {
        if (assessmentPolicyId == null) {
            return List.of();
        }

        var policy = assessmentPolicyRepository.findById(assessmentPolicyId).orElse(null);
        if (policy == null) {
            return List.of();
        }
        if (policy.getTargetFrameworkBandId() == null) {
            throw new IllegalStateException("Assessment policy chưa cấu hình bậc mục tiêu để AI chấm.");
        }
        var targetBand = frameworkResultBandRepository.findById(policy.getTargetFrameworkBandId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bậc mục tiêu của assessment policy."));
        if (!policy.getFrameworkVersionId().equals(targetBand.getFrameworkVersionId())) {
            throw new IllegalStateException("Bậc mục tiêu không thuộc framework version của assessment policy.");
        }

        var rubricCriteria = rubricCriterionRepository.findByRubricVersionId(policy.getRubricVersionId());
        if (rubricCriteria.isEmpty()) {
            return List.of();
        }

        var frameworkCriterionIds = rubricCriteria.stream()
            .map(item -> item.getFrameworkCriterionId())
            .filter(id -> id != null)
            .toList();
        var frameworkCriteriaById = frameworkCriterionRepository.findAllByIds(frameworkCriterionIds).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity()));
        var frameworkCriterionBands = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(frameworkCriterionIds);
        var frameworkBandsByCriterionId = frameworkCriterionBands.stream()
            .collect(Collectors.groupingBy(item -> item.getFrameworkCriterionId()));
        var frameworkResultBandIds = frameworkCriterionBands.stream()
            .map(item -> item.getFrameworkResultBandId())
            .distinct()
            .toList();
        var frameworkResultBandsById = frameworkResultBandRepository.findAllByIds(frameworkResultBandIds).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity()));
        var ladderCriterionKeys = Set.of("grammar", "vocabulary", "coherence");

        return rubricCriteria.stream().map(criterion -> {
            if (criterion.getMinScore() == null || criterion.getMaxScore() == null
                    || criterion.getMinScore().compareTo(criterion.getMaxScore()) >= 0) {
                throw new IllegalStateException("Tiêu chí rubric '" + criterion.getName()
                    + "' phải có minScore nhỏ hơn maxScore trước khi gửi sang AI.");
            }
            var frameworkCriterion = frameworkCriteriaById.get(criterion.getFrameworkCriterionId());
            var criterionBands = frameworkBandsByCriterionId
                .getOrDefault(criterion.getFrameworkCriterionId(), List.of());
            var targetCriterionBand = criterionBands.stream()
                .filter(item -> policy.getTargetFrameworkBandId().equals(item.getFrameworkResultBandId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Tiêu chí framework chưa có mô tả cho bậc mục tiêu " + targetBand.getLabel() + "."));
            var criterionKey = agentCriterionKey(criterion.getCode());
            var sourceBands = criterionKey != null && ladderCriterionKeys.contains(criterionKey)
                ? criterionBands
                : List.of(targetCriterionBand);
            var bands = sourceBands.stream()
                .map(item -> {
                    var resultBand = frameworkResultBandsById.get(item.getFrameworkResultBandId());
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
        var normalized = rubricCriterionCode.trim().toLowerCase(Locale.ROOT);
        return "discourse".equals(normalized) ? "coherence" : normalized;
    }
}
