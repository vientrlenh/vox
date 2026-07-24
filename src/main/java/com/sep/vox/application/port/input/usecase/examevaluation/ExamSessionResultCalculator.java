package com.sep.vox.application.port.input.usecase.examevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamSession;
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
        var loaded = load(sessionId);
        var evaluationsByResponseId = evaluationsByResponseId(loaded.responses());

        var itemScoreByResponseId = new LinkedHashMap<UUID, BigDecimal>();
        for (var response : loaded.responses()) {
            var evaluation = evaluationsByResponseId.get(response.getId());
            if (evaluation == null) {
                throw new NotFoundException("Khong tim thay evaluation cho cau tra loi " + response.getId());
            }
            itemScoreByResponseId.put(response.getId(),
                evaluation.getItemScore() == null ? BigDecimal.ZERO : evaluation.getItemScore());
        }

        var rolled = rollUp(loaded, itemScoreByResponseId);
        var anyRequiresHumanReview = evaluationsByResponseId.values().stream()
            .anyMatch(evaluation -> evaluation.isRequiresHumanReview());
        var targetFrameworkBand = loaded.policy().getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(loaded.policy().getTargetFrameworkBandId()).orElse(null);

        return new CalculatedExamSessionResult(
            loaded.session().getId(),
            loaded.examId(),
            loaded.session().getPaperId(),
            loaded.session().getCandidateId(),
            loaded.policy(),
            rolled.totalScore(),
            targetFrameworkBand,
            resolveRubricResultBand(loaded.policy(), rolled.totalScore()),
            rolled.sections(),
            rolled.items(),
            anyRequiresHumanReview
        );
    }

    /**
     * Tổng điểm "nếu nộp bộ điểm này" — cùng một công thức với {@link #calculate},
     * nhưng KHÔNG ghi gì. Màn chấm tay cần nó vì tổng là weighted theo item rồi
     * theo section (không phải trung bình cộng), FE không tự tính khớp được.
     *
     * <p>{@code overrides} là điểm giáo viên đang nhập, khoá theo responseId. Response
     * nào không có override thì lấy điểm của bản chấm đang có hiệu lực — nhờ vậy
     * chấm dở vẫn preview được và phần chưa nhập giữ nguyên điểm AI.
     */
    public PreviewedExamSessionResult preview(UUID sessionId, Map<UUID, BigDecimal> overrides) {
        var loaded = load(sessionId);
        var safeOverrides = overrides == null ? Map.<UUID, BigDecimal>of() : overrides;
        var evaluationsByResponseId = evaluationsByResponseId(loaded.responses());

        var itemScoreByResponseId = new LinkedHashMap<UUID, BigDecimal>();
        for (var response : loaded.responses()) {
            var override = safeOverrides.get(response.getId());
            if (override != null) {
                itemScoreByResponseId.put(response.getId(), override);
                continue;
            }
            var evaluation = evaluationsByResponseId.get(response.getId());
            if (evaluation == null) {
                throw new NotFoundException("Khong tim thay evaluation cho cau tra loi " + response.getId());
            }
            itemScoreByResponseId.put(response.getId(),
                evaluation.getItemScore() == null ? BigDecimal.ZERO : evaluation.getItemScore());
        }

        var rolled = rollUp(loaded, itemScoreByResponseId);
        var band = resolveRubricResultBand(loaded.policy(), rolled.totalScore());
        return new PreviewedExamSessionResult(
            rolled.totalScore(),
            band == null ? null : band.getName(),
            rolled.sections(),
            rolled.items()
        );
    }

    /**
     * Phần thuần tính toán: item -> section -> total. Không chạm repository, nên
     * {@code calculate} và {@code preview} dùng chung đúng một công thức.
     */
    private RolledUpScores rollUp(LoadedSession loaded, Map<UUID, BigDecimal> itemScoreByResponseId) {
        var sectionScores = new LinkedHashMap<UUID, BigDecimal>();
        loaded.orderedSections().forEach(section -> sectionScores.put(section.getId(), scaled(BigDecimal.ZERO)));

        var itemScores = new ArrayList<ItemScore>();
        for (var response : loaded.responses()) {
            var paperItem = loaded.paperItemsById().get(response.getPaperItemId());
            if (paperItem == null) {
                throw new NotFoundException("Khong tim thay paper item " + response.getPaperItemId());
            }
            var rawScore = itemScoreByResponseId.getOrDefault(response.getId(), BigDecimal.ZERO);
            var itemScore = scaled(rawScore == null ? BigDecimal.ZERO : rawScore);
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

        var sections = loaded.orderedSections().stream()
            .map(section -> new SectionScore(
                section.getId(),
                section.getTitle(),
                sectionScores.getOrDefault(section.getId(), scaled(BigDecimal.ZERO))
            ))
            .toList();
        return new RolledUpScores(
            calculateTotalScore(loaded.orderedSections(), sectionScores), sections, itemScores);
    }

    /** Tất cả dữ liệu một lần chấm cần, nạp đúng một lần cho cả calculate lẫn preview. */
    private LoadedSession load(UUID sessionId) {
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
        var responses = examItemResponseRepository.findBySessionId(sessionId);
        var paperItemsById = examPaperItemRepository.findByPaperId(session.getPaperId()).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity(), (left, right) -> left));

        return new LoadedSession(session, exam.getId(), policy, orderedSections, responses, paperItemsById);
    }

    /**
     * Batched (not one findLatestByResponseId/findById per response, which was
     * a real N+1 -- this runs on every GET of an exam result, not just once at
     * grading time).
     */
    private Map<UUID, ExamItemEvaluation> evaluationsByResponseId(List<ExamItemResponse> responses) {
        var responseIds = responses.stream().map(response -> response.getId()).toList();
        return examItemEvaluationRepository.findLatestByResponseIdIn(responseIds).stream()
            .collect(Collectors.toMap(evaluation -> evaluation.getResponseId(), Function.identity(), (left, right) -> left));
    }

    private RubricResultBand resolveRubricResultBand(AssessmentPolicy policy, BigDecimal totalScore) {
        return rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(band -> band.getOrder()))
            .filter(band -> within(totalScore, band.getScoreMin(), band.getScoreMax()))
            .findFirst()
            .orElse(null);
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
            List<ExamPaperSection> orderedSections,
            LinkedHashMap<UUID, BigDecimal> sectionScores) {
        if (orderedSections.isEmpty()) {
            return scaled(BigDecimal.ZERO);
        }

        var totalWeight = orderedSections.stream()
            .map(section -> section.getWeight() == null ? BigDecimal.ZERO : section.getWeight())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            var weightedTotal = orderedSections.stream()
                .map(section -> sectionScores.getOrDefault(section.getId(), scaled(BigDecimal.ZERO))
                    .multiply(section.getWeight() == null ? BigDecimal.ZERO : section.getWeight()))
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
            return scaled(weightedTotal.divide(totalWeight, 2, RoundingMode.HALF_UP));
        }

        var plainTotal = sectionScores.values().stream().reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        return scaled(plainTotal.divide(BigDecimal.valueOf(sectionScores.size()), 2, RoundingMode.HALF_UP));
    }

    private record LoadedSession(
        ExamSession session,
        UUID examId,
        AssessmentPolicy policy,
        List<ExamPaperSection> orderedSections,
        List<ExamItemResponse> responses,
        Map<UUID, ExamPaperItem> paperItemsById
    ) {
    }

    private record RolledUpScores(
        BigDecimal totalScore,
        List<SectionScore> sections,
        List<ItemScore> items
    ) {
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
        List<ItemScore> items,
        boolean anyRequiresHumanReview
    ) {
    }

    /** Kết quả "chấm thử": chỉ những gì màn chấm cần, không có gì để ghi xuống DB. */
    public record PreviewedExamSessionResult(
        BigDecimal totalScore,
        String resultBandName,
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
