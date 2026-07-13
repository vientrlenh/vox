package com.sep.vox.application.port.input.usecase.examevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

@Service
public class ExamSessionResultCalculator {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    public ExamSessionResultCalculator(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            RubricResultBandRepository rubricResultBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    public CalculatedExamSessionResult calculate(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay phien thi"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay bai kiem tra"));
        if (exam.getAssessmentPolicyId() == null) {
            throw new NotFoundException("Bai kiem tra chua gan assessment policy");
        }
        var policy = assessmentPolicyRepository.findById(exam.getAssessmentPolicyId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay assessment policy"));

        var orderedSections = examPaperSectionRepository.findByPaperId(session.getPaperId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        var sectionScores = new LinkedHashMap<UUID, BigDecimal>();
        orderedSections.forEach(section -> sectionScores.put(section.getId(), scaled(BigDecimal.ZERO)));

        // Batched (not one findLatestByResponseId/findById per response, which was
        // a real N+1 -- this calculate() runs on every GET of an exam result, not
        // just once at grading time) -- 2 queries total instead of 2 per response.
        var responses = examItemResponseRepository.findBySessionId(sessionId);
        var responseIds = responses.stream().map(response -> response.getId()).toList();
        var evaluationsByResponseId = examItemEvaluationRepository.findLatestByResponseIdIn(responseIds).stream()
            .collect(Collectors.toMap(ExamItemEvaluation::getResponseId, Function.identity(), (left, right) -> left));
        var paperItemsById = examPaperItemRepository.findByPaperId(session.getPaperId()).stream()
            .collect(Collectors.toMap(ExamPaperItem::getId, Function.identity(), (left, right) -> left));

        var itemScores = new ArrayList<ItemScore>();
        for (var response : responses) {
            var evaluation = evaluationsByResponseId.get(response.getId());
            if (evaluation == null) {
                throw new NotFoundException("Khong tim thay evaluation cho cau tra loi " + response.getId());
            }
            var paperItem = paperItemsById.get(response.getPaperItemId());
            if (paperItem == null) {
                throw new NotFoundException("Khong tim thay paper item " + response.getPaperItemId());
            }
            var itemScore = scaled(evaluation.getItemScore() == null ? BigDecimal.ZERO : evaluation.getItemScore());
            var weight = paperItem.getWeight() == null ? BigDecimal.ZERO : paperItem.getWeight();
            var weightedScore = scaled(itemScore.multiply(weight));
            sectionScores.compute(paperItem.getSectionId(), (ignored, current) ->
                scaled((current == null ? BigDecimal.ZERO : current).add(weightedScore))
            );
            itemScores.add(new ItemScore(
                paperItem.getId(),
                response.getId(),
                paperItem.getSectionId(),
                itemScore,
                weightedScore
            ));
        }

        var totalScore = calculateTotalScore(orderedSections, sectionScores);
        var rubricResultBand = rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(RubricResultBand::getOrder))
            .filter(band -> within(totalScore, band.getScoreMin(), band.getScoreMax()))
            .findFirst()
            .orElse(null);
        var targetFrameworkBand = policy.getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(policy.getTargetFrameworkBandId()).orElse(null);

        return new CalculatedExamSessionResult(
            session.getId(),
            exam.getId(),
            session.getPaperId(),
            session.getCandidateId(),
            policy,
            totalScore,
            targetFrameworkBand,
            rubricResultBand,
            orderedSections.stream()
                .map(section -> new SectionScore(
                    section.getId(),
                    section.getTitle(),
                    sectionScores.getOrDefault(section.getId(), scaled(BigDecimal.ZERO))
                ))
                .toList(),
            itemScores
        );
    }

    private boolean within(BigDecimal score, BigDecimal min, BigDecimal max) {
        if (score == null || min == null || max == null) {
            return false;
        }
        return score.compareTo(min) >= 0 && score.compareTo(max) <= 0;
    }

    private BigDecimal scaled(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalScore(
            List<com.sep.vox.domain.model.exam.ExamPaperSection> orderedSections,
            LinkedHashMap<UUID, BigDecimal> sectionScores) {
        if (orderedSections.isEmpty()) {
            return scaled(BigDecimal.ZERO);
        }

        var totalWeight = orderedSections.stream()
            .map(section -> section.getWeight() == null ? BigDecimal.ZERO : section.getWeight())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            var weightedTotal = orderedSections.stream()
                .map(section -> sectionScores.getOrDefault(section.getId(), scaled(BigDecimal.ZERO))
                    .multiply(section.getWeight() == null ? BigDecimal.ZERO : section.getWeight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            return scaled(weightedTotal.divide(totalWeight, 2, RoundingMode.HALF_UP));
        }

        var plainTotal = sectionScores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return scaled(plainTotal.divide(BigDecimal.valueOf(sectionScores.size()), 2, RoundingMode.HALF_UP));
    }

    public record CalculatedExamSessionResult(
        UUID sessionId,
        UUID examId,
        UUID paperId,
        UUID candidateId,
        AssessmentPolicy policy,
        BigDecimal totalScore,
        FrameworkResultBand targetFrameworkBand,
        RubricResultBand rubricResultBand,
        List<SectionScore> sections,
        List<ItemScore> items
    ) {
    }

    public record SectionScore(
        UUID sectionId,
        String title,
        BigDecimal score
    ) {
    }

    public record ItemScore(
        UUID paperItemId,
        UUID responseId,
        UUID sectionId,
        BigDecimal itemScore,
        BigDecimal weightedScore
    ) {
    }
}
