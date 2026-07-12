package com.sep.vox.application.port.input.usecase.examsession;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.event.ExamAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
public class SubmitExamSessionUseCase implements IUseCase<SubmitExamSessionCommand, Void> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final ExternalEventPublisherPort externalEventPublisherPort;

    public SubmitExamSessionUseCase(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionTopicRepository questionTopicRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricCriterionBandRepository rubricCriterionBandRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            ExternalEventPublisherPort externalEventPublisherPort) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.externalEventPublisherPort = externalEventPublisherPort;
    }

    @Override
    public Void execute(SubmitExamSessionCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không thể tìm thấy phiên thi"));
        if (session.getStatus() != ExamSessionStatus.SUBMITTED && session.getStatus() != ExamSessionStatus.EXPIRED) {
            throw new IllegalStateException("chỉ được gửi chấm khi phiên thi đã nộp hoặc hết giờ");
        }

        var responses = examItemResponseRepository.findBySessionId(session.getId());
        if (responses.isEmpty()) {
            session.setStatus(ExamSessionStatus.GRADING);
            session = examSessionRepository.save(session);
            session.setStatus(ExamSessionStatus.GRADED);
            examSessionRepository.save(session);
            return null;
        }

        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy bài kiểm tra"));
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

        session.setStatus(ExamSessionStatus.GRADING);
        examSessionRepository.save(session);
        return null;
    }

    private List<ExamAttemptEvaluationRequestedExternalEvent.CriterionFramework> buildCriteriaFrameworks(UUID assessmentPolicyId) {
        if (assessmentPolicyId == null) {
            return List.of();
        }

        var policy = assessmentPolicyRepository.findById(assessmentPolicyId).orElse(null);
        if (policy == null) {
            return List.of();
        }

        var rubricCriteria = rubricCriterionRepository.findByRubricVersionId(policy.getRubricVersionId());
        if (rubricCriteria.isEmpty()) {
            return List.of();
        }

        var criterionIds = rubricCriteria.stream().map(item -> item.getId()).toList();
        var frameworkCriterionIds = rubricCriteria.stream()
            .map(item -> item.getFrameworkCriterionId())
            .filter(id -> id != null)
            .toList();
        var rubricBandsByCriterionId = rubricCriterionBandRepository.findByCriterionIdIn(criterionIds).stream()
            .collect(Collectors.groupingBy(item -> item.getCriterionId()));
        var frameworkCriteriaById = frameworkCriterionRepository.findAllByIds(frameworkCriterionIds).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity()));
        var frameworkBandsByCriterionId = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(frameworkCriterionIds).stream()
            .collect(Collectors.groupingBy(item -> item.getFrameworkCriterionId()));
        var resultBandIds = frameworkBandsByCriterionId.values().stream()
            .flatMap(List::stream)
            .map(item -> item.getFrameworkResultBandId())
            .distinct()
            .toList();
        var resultBandsById = frameworkResultBandRepository.findAllByIds(resultBandIds).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity()));

        return rubricCriteria.stream().map(criterion -> {
            var frameworkCriterion = frameworkCriteriaById.get(criterion.getFrameworkCriterionId());
            var rubricBands = rubricBandsByCriterionId.getOrDefault(criterion.getId(), List.of()).stream()
                .collect(Collectors.toMap(item -> item.getCode().trim().toUpperCase(), Function.identity(), (left, right) -> left));
            var bands = frameworkBandsByCriterionId.getOrDefault(criterion.getFrameworkCriterionId(), List.of()).stream()
                .sorted(Comparator.comparing(item -> {
                    var resultBand = resultBandsById.get(item.getFrameworkResultBandId());
                    return resultBand == null ? Integer.MAX_VALUE : resultBand.getOrder();
                }))
                .map(item -> {
                    var resultBand = resultBandsById.get(item.getFrameworkResultBandId());
                    var rubricBand = resultBand == null ? null : rubricBands.get(resultBand.getCode().trim().toUpperCase());
                    return new ExamAttemptEvaluationRequestedExternalEvent.FrameworkBand(
                        resultBand == null ? null : resultBand.getCode(),
                        resultBand == null ? null : resultBand.getLabel(),
                        rubricBand == null ? 0D : rubricBand.getScoreMin().doubleValue(),
                        rubricBand == null ? 100D : rubricBand.getScoreMax().doubleValue(),
                        item.getDescriptor(),
                        item.getPositiveSignals() == null ? List.of() : item.getPositiveSignals().values().stream().map(signal -> signal.description()).toList(),
                        item.getNegativeSignals() == null ? List.of() : item.getNegativeSignals().values().stream().map(signal -> signal.description()).toList()
                    );
                })
                .toList();

            return new ExamAttemptEvaluationRequestedExternalEvent.CriterionFramework(
                criterion.getCode() == null ? null : criterion.getCode().trim().toLowerCase(Locale.ROOT),
                frameworkCriterion == null ? null : frameworkCriterion.getCode(),
                frameworkCriterion == null ? null : frameworkCriterion.getName(),
                frameworkCriterion == null ? null : frameworkCriterion.getDescription(),
                criterion.getWeight() == null ? null : criterion.getWeight().doubleValue(),
                criterion.getMinScore() == null ? 0D : criterion.getMinScore().doubleValue(),
                criterion.getMaxScore() == null ? 100D : criterion.getMaxScore().doubleValue(),
                bands
            );
        }).toList();
    }
}
