package com.sep.vox.application.port.input.usecase.examsession;

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

import com.sep.vox.application.common.ExamCandidateStatusSupport;
import com.sep.vox.application.event.ExamAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.service.MissingResponseBackfillService;
import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
public class SubmitExamSessionUseCase implements IUseCase<SubmitExamSessionCommand, Void> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final MissingResponseBackfillService missingResponseBackfillService;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
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
    private final ZeroScoreExamResultService zeroScoreExamResultService;

    public SubmitExamSessionUseCase(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamItemResponseRepository examItemResponseRepository,
            MissingResponseBackfillService missingResponseBackfillService,
            ExamItemResponseTurnRepository examItemResponseTurnRepository,
            ExamPaperItemRepository examPaperItemRepository,
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
            ZeroScoreExamResultService zeroScoreExamResultService) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.missingResponseBackfillService = missingResponseBackfillService;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
        this.examPaperItemRepository = examPaperItemRepository;
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
        this.zeroScoreExamResultService = zeroScoreExamResultService;
    }

    @Override
    @Transactional
    public Void execute(SubmitExamSessionCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không thể tìm thấy phiên thi"));
        // GRADED chỉ được chấp nhận ở đây cho đúng 1 trường hợp: G.4 - session từng bị đánh
        // dấu vi phạm oan (INVALID, chưa từng có ExamItemEvaluation), đã dỡ cấm, được
        // RetryGradingExamSessionUseCase gọi lại để AI chấm thật lần đầu.
        var fromStatus = session.getStatus();
        if (fromStatus != ExamSessionStatus.SUBMITTED
                && fromStatus != ExamSessionStatus.EXPIRED
                && fromStatus != ExamSessionStatus.GRADING_FAILED
                && fromStatus != ExamSessionStatus.GRADED) {
            throw new IllegalStateException("chỉ được gửi chấm khi phiên thi đã nộp hoặc hết giờ");
        }

       
        boolean claimed = examSessionRepository.tryTransitionStatus(
            session.getId(), fromStatus, ExamSessionStatus.GRADING);
        if (!claimed) {
            return null;
        }
        session.setStatus(ExamSessionStatus.GRADING);

        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy thí sinh của phiên thi"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy bài kiểm tra"));
        var responses = examItemResponseRepository.findBySessionId(session.getId());
        if (candidate.getBlockedAt() != null) {
            persistInvalidBlockedResult(session);
            return null;
        }
        // Áp dụng cho mọi loại bài: cổng vào thi đã chặn người chưa điểm danh, đây là lớp chốt thứ hai.
        if (!ExamCandidateStatusSupport.isAttended(candidate.getStatus())) {
            zeroScoreExamResultService.releaseZeroForEmptySession(session.getId());
            session.setStatus(ExamSessionStatus.GRADED);
            examSessionRepository.save(session);
            return null;
        }
        if (responses.isEmpty()) {
            zeroScoreExamResultService.releaseZeroForEmptySession(session.getId());
            session.setStatus(ExamSessionStatus.GRADED);
            examSessionRepository.save(session);
            return null;
        }

        // Lấp câu KHÔNG có bản ghi (hết giờ, mất kết nối, buộc kết thúc) bằng cặp response rỗng
        // + bản chấm 0 điểm. Đặt SAU hai nhánh thoát sớm ở trên vì cả hai đã tự xử trọn bài
        // (releaseZeroForEmptySession), và TRƯỚC vòng bắn sự kiện AI bên dưới -- vòng đó chạy
        // trên `responses` đọc lúc nãy nên các dòng vừa tạo không lọt vào, đúng ý: không đẩy
        // transcript rỗng sang LLM. Xem MissingResponseBackfillService.
        missingResponseBackfillService.backfill(session.getId(), session.getPaperId());

        var criteriaFrameworks = buildCriteriaFrameworks(exam.getAssessmentPolicyId());

        for (var response : responses) {
            var paperItemId = response.getPaperItemId();
            if (paperItemId == null) {
                throw new NotFoundException("không thể tìm thấy paperItemId cho câu trả lời " + response.getId());
            }

            var paperItem = examPaperItemRepository.findById(paperItemId)
                .orElseThrow(() -> new NotFoundException("không thể tìm thấy paper item " + paperItemId));
            var question = questionRepository.findById(paperItem.getQuestionId())
                .orElseThrow(() -> new NotFoundException("không thể tìm thấy câu hỏi " + paperItem.getQuestionId()));
            var guide = questionEvaluationGuideRepository.findByQuestionId(question.getId()).orElse(null);
            var asset = questionAssetRepository.findByQuestionId(question.getId()).stream().findFirst().orElse(null);
            var topic = question.getQuestionTopicId() == null
                ? null
                : questionTopicRepository.findById(question.getQuestionTopicId()).orElse(null);
            var turns = examItemResponseTurnRepository.findByExamItemResponseId(response.getId()).stream()
                .sorted(Comparator.comparingInt(turn -> turn.getTurnOrder()))
                .map(turn -> new ExamAttemptEvaluationRequestedExternalEvent.TurnInput(
                    turn.getTurnOrder(),
                    turn.getTurnType().name(),
                    turn.getPromptText(),
                    turn.getAudioUrl(),
                    turn.getTranscript(),
                    turn.getDurationSeconds()
                ))
                .toList();
            if (turns.isEmpty()) {
                turns = List.of(new ExamAttemptEvaluationRequestedExternalEvent.TurnInput(
                    1,
                    "MAIN",
                    null,
                    response.getAudioUrl(),
                    response.getTranscript(),
                    response.getDurationSeconds()
                ));
            }

            var event = new ExamAttemptEvaluationRequestedExternalEvent(
                session.getId().toString(),
                response.getId().toString(),
                question.getId().toString(),
                new ExamAttemptEvaluationRequestedExternalEvent.Payload(
                    question.getQuestionText(),
                    question.getType() == null ? null : question.getType().name(),
                    null,
                    question.getMaxResponseSeconds(),
                    question.getMinResponseSeconds(),
                    question.getMaxResponseSeconds(),
                    asset == null ? null : new ExamAttemptEvaluationRequestedExternalEvent.Asset(
                        asset.getType() == null ? null : asset.getType().name(),
                        asset.getTranscript(),
                        asset.getDescription(),
                        asset.getAltText()
                    ),
                    topic == null ? null : topic.getName(),
                    topic == null ? null : topic.getDescription(),
                    guide == null ? null : new ExamAttemptEvaluationRequestedExternalEvent.EvaluationGuide(
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

    private void persistInvalidBlockedResult(com.sep.vox.domain.model.exam.ExamSession session) {
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy bài kiểm tra"));
        var policy = exam.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(exam.getAssessmentPolicyId()).orElse(null);
        var existing = examCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        var result = existing == null ? new ExamCandidateResult() : existing;
        var now = Instant.now();

        result.setExamId(session.getExamId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy == null ? null : policy.getId());
        result.setPolicyVersion(policy == null ? 0 : policy.getVersion());
        result.setRubricVersionId(policy == null ? null : policy.getRubricVersionId());
        result.setFrameworkVersionId(policy == null ? null : policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy == null ? null : policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(null);
        result.setTotalScore(null);
        result.setStatus(ExamCandidateResultStatus.INVALID);
        result.setFinalizedAt(now);
        if (existing == null) {
            result.setCreatedAt(now);
            result.setCreatedBy(null);
        }
        result.setUpdatedAt(now);
        result.setUpdatedBy(null);
        examCandidateResultRepository.save(result);

        session.setStatus(ExamSessionStatus.GRADED);
        examSessionRepository.save(session);
    }

    private List<ExamAttemptEvaluationRequestedExternalEvent.CriterionFramework> buildCriteriaFrameworks(UUID assessmentPolicyId) {
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
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bậc mục tiêu của assessment policy."));
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
                    return new ExamAttemptEvaluationRequestedExternalEvent.FrameworkBand(
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

            return new ExamAttemptEvaluationRequestedExternalEvent.CriterionFramework(
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
