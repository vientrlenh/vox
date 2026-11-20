package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import com.sep.vox.domain.model.assessment.Assessment;
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
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
public class AssessmentSessionResultCalculator {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(AssessmentSessionResultCalculator.class);

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;

    public AssessmentSessionResultCalculator(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentRepository assessmentRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            AssessmentItemResponseRepository assessmentItemResponseRepository,
            AssessmentItemEvaluationRepository assessmentItemEvaluationRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            RubricResultBandRepository rubricResultBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricVersionRepository rubricVersionRepository) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
        this.assessmentItemEvaluationRepository = assessmentItemEvaluationRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
    }

    public CalculatedAssessmentSessionResult calculate(UUID sessionId) {
        LoadedSession loaded = load(sessionId);
        Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId = evaluationsByResponseId(loaded.responses());

        Map<UUID, BigDecimal> itemScoreByResponseId = new LinkedHashMap<UUID, BigDecimal>();
        for (AssessmentItemResponse response : loaded.responses()) {
            AssessmentItemEvaluation evaluation = evaluationsByResponseId.get(response.getId());
            if (evaluation == null) {
                throw new NotFoundException("Khong tim thay evaluation cho cau tra loi " + response.getId());
            }
            itemScoreByResponseId.put(response.getId(),
                evaluation.getItemScore() == null ? BigDecimal.ZERO : evaluation.getItemScore());
        }

        RolledUpScores rolled = rollUp(loaded, itemScoreByResponseId);
        boolean anyRequiresHumanReview = evaluationsByResponseId.values().stream()
            .anyMatch(evaluation -> evaluation.isRequiresHumanReview());
        FrameworkResultBand targetFrameworkBand = loaded.policy().getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(loaded.policy().getTargetFrameworkBandId()).orElse(null);

        return new CalculatedAssessmentSessionResult(
            loaded.session().getId(),
            loaded.assessmentId(),
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
    public PreviewedAssessmentSessionResult preview(UUID sessionId, Map<UUID, BigDecimal> overrides) {
        LoadedSession loaded = load(sessionId);
        Map<UUID, BigDecimal> safeOverrides = overrides == null ? Map.<UUID, BigDecimal>of() : overrides;
        Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId = evaluationsByResponseId(loaded.responses());

        Map<UUID, BigDecimal> itemScoreByResponseId = new LinkedHashMap<UUID, BigDecimal>();
        for (AssessmentItemResponse response : loaded.responses()) {
            BigDecimal override = safeOverrides.get(response.getId());
            if (override != null) {
                itemScoreByResponseId.put(response.getId(), override);
                continue;
            }
            AssessmentItemEvaluation evaluation = evaluationsByResponseId.get(response.getId());
            if (evaluation == null) {
                throw new NotFoundException("Khong tim thay evaluation cho cau tra loi " + response.getId());
            }
            itemScoreByResponseId.put(response.getId(),
                evaluation.getItemScore() == null ? BigDecimal.ZERO : evaluation.getItemScore());
        }

        RolledUpScores rolled = rollUp(loaded, itemScoreByResponseId);
        RubricResultBand band = resolveRubricResultBand(loaded.policy(), rolled.totalScore());
        return new PreviewedAssessmentSessionResult(
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
        LinkedHashMap<UUID, BigDecimal> sectionScores = new LinkedHashMap<UUID, BigDecimal>();
        // Tổng trọng số câu THEO TỪNG PHẦN -- mẫu số của phép chuẩn hoá bên dưới.
        Map<UUID, BigDecimal> sectionWeightSums = new LinkedHashMap<UUID, BigDecimal>();
        // Tổng điểm câu KHÔNG nhân trọng số, và số câu -- chỉ dùng khi mọi trọng số trong phần đều
        // bằng 0, lúc đó lùi về trung bình cộng (xem normalizedSectionScore).
        Map<UUID, BigDecimal> sectionPlainSums = new LinkedHashMap<UUID, BigDecimal>();
        Map<UUID, Integer> sectionItemCounts = new LinkedHashMap<UUID, Integer>();
        loaded.orderedSections().forEach(section -> {
            sectionScores.put(section.getId(), scaled(BigDecimal.ZERO));
            sectionWeightSums.put(section.getId(), BigDecimal.ZERO);
            sectionPlainSums.put(section.getId(), BigDecimal.ZERO);
            sectionItemCounts.put(section.getId(), 0);
        });

        List<ItemScore> itemScores = new ArrayList<ItemScore>();
        for (AssessmentItemResponse response : loaded.responses()) {
            AssessmentPaperItem paperItem = loaded.paperItemsById().get(response.getPaperItemId());
            if (paperItem == null) {
                throw new NotFoundException("Khong tim thay paper item " + response.getPaperItemId());
            }
            BigDecimal rawScore = itemScoreByResponseId.getOrDefault(response.getId(), BigDecimal.ZERO);
            BigDecimal itemScore = scaled(rawScore == null ? BigDecimal.ZERO : rawScore);
            BigDecimal weight = paperItem.getWeight() == null ? BigDecimal.ZERO : paperItem.getWeight();
            BigDecimal weightedScore = scaled(itemScore.multiply(weight));
            sectionScores.compute(paperItem.getSectionId(), (ignored, current) ->
                scaled((current == null ? BigDecimal.ZERO : current).add(weightedScore))
            );
            sectionWeightSums.compute(paperItem.getSectionId(), (ignored, current) ->
                (current == null ? BigDecimal.ZERO : current).add(weight)
            );
            sectionPlainSums.compute(paperItem.getSectionId(), (ignored, current) ->
                (current == null ? BigDecimal.ZERO : current).add(itemScore)
            );
            sectionItemCounts.merge(paperItem.getSectionId(), 1, (a, b) -> Integer.sum(a, b));
            itemScores.add(new ItemScore(
                paperItem.getId(),
                response.getId(),
                paperItem.getSectionId(),
                paperItem.getQuestionId(),
                itemScore,
                weightedScore
            ));
        }

        // sectionScores được dùng lại ở calculateTotalScore, nên chuẩn hoá TẠI CHỖ trước khi dựng
        // danh sách hiển thị -- không thì điểm phần người dùng thấy và điểm phần đi vào tổng là hai
        // số khác nhau.
        sectionScores.replaceAll((sectionId, weightedSum) -> normalizedSectionScore(
            weightedSum,
            sectionWeightSums.getOrDefault(sectionId, BigDecimal.ZERO),
            sectionPlainSums.getOrDefault(sectionId, BigDecimal.ZERO),
            sectionItemCounts.getOrDefault(sectionId, 0),
            loaded.scaleFloor()
        ));

        List<SectionScore> sections = loaded.orderedSections().stream()
            .map(section -> new SectionScore(
                section.getId(),
                section.getTitle(),
                sectionScores.getOrDefault(section.getId(), loaded.scaleFloor())
            ))
            .toList();
        return new RolledUpScores(
            calculateTotalScore(loaded.orderedSections(), sectionScores, loaded.scaleFloor()),
            sections,
            itemScores);
    }

    /** Tất cả dữ liệu một lần chấm cần, nạp đúng một lần cho cả calculate lẫn preview. */
    private LoadedSession load(UUID sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay phien thi"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay bai kiem tra"));
        if (assessment.getAssessmentPolicyId() == null) {
            throw new NotFoundException("Bai kiem tra chua gan assessment policy");
        }
        AssessmentPolicy policy = assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay assessment policy"));

        List<AssessmentPaperSection> orderedSections = assessmentPaperSectionRepository.findByPaperId(session.getPaperId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        Map<UUID, AssessmentPaperItem> paperItemsById = assessmentPaperItemRepository.findByPaperId(session.getPaperId()).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity(), (left, right) -> left));

        // Sắp NGAY TẠI ĐÂY chứ không ở chỗ dựng danh sách hiển thị: cả ba vòng lặp trong lớp này
        // (calculate, preview, rollUp) đều duyệt `responses`, nên sắp ở nguồn thì mọi đường ra đều
        // cùng một thứ tự.
        //
        // findBySessionId không có ORDER BY, tức thứ tự là do Postgres trả về sao thì lấy vậy.
        // Quan sát thật (phiên 019fff59, 2026-08-14): trả về câu của Part 2 trước câu của Part 1,
        // mà cũng không theo thời gian nộp (Part 1 nộp lúc 08:19, Part 2 lúc 08:21). Client đánh
        // số "Câu {index + 1}" theo vị trí mảng nên học sinh thấy Câu 1 mang điểm của Part 2.
        //
        // Phải so SECTION TRƯỚC rồi mới tới item: đề hai phần mỗi phần một câu thì cả hai câu đều
        // có order = 1, chỉ so item là hoà và thứ tự lại thành tuỳ ý như cũ. Chốt bằng id để hai
        // lần gọi liên tiếp không ra hai thứ tự khác nhau khi dữ liệu lỗi làm hoà cả hai khoá.
        Map<UUID, Integer> sectionRankById = new java.util.HashMap<UUID, Integer>();
        for (int index = 0; index < orderedSections.size(); index++) {
            sectionRankById.put(orderedSections.get(index).getId(), index);
        }
        List<AssessmentItemResponse> responses = assessmentItemResponseRepository.findBySessionId(sessionId).stream()
            .sorted(Comparator
                .<AssessmentItemResponse>comparingInt(response -> {
                    AssessmentPaperItem paperItem = paperItemsById.get(response.getPaperItemId());
                    return paperItem == null
                        ? Integer.MAX_VALUE
                        : sectionRankById.getOrDefault(paperItem.getSectionId(), Integer.MAX_VALUE);
                })
                .thenComparingInt(response -> {
                    AssessmentPaperItem paperItem = paperItemsById.get(response.getPaperItemId());
                    return paperItem == null ? Integer.MAX_VALUE : paperItem.getOrder();
                })
                .thenComparing(response -> response.getId()))
            .toList();

        return new LoadedSession(
            session, assessment.getId(), policy, orderedSections, responses, paperItemsById,
            resolveScaleFloor(policy));
    }

    /**
     * Batched (not one findLatestByResponseId/findById per response, which was
     * a real N+1 -- this runs on every GET of an assessment result, not just once at
     * grading time).
     */
    private Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId(List<AssessmentItemResponse> responses) {
        List<UUID> responseIds = responses.stream().map(response -> response.getId()).toList();
        return assessmentItemEvaluationRepository.findLatestByResponseIdIn(responseIds).stream()
            .collect(Collectors.toMap(evaluation -> evaluation.getResponseId(), Function.identity(), (left, right) -> left));
    }

    private RubricResultBand resolveRubricResultBand(AssessmentPolicy policy, BigDecimal totalScore) {
        if (policy.getRubricVersionId() == null) {
            return null;
        }
        List<RubricResultBand> matchingBands = rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(band -> band.getOrder()))
            .filter(band -> within(totalScore, band.getScoreMin(), band.getScoreMax()))
            .toList();
        if (matchingBands.isEmpty()) {
            LOGGER.info(
                "Không có dải điểm kết quả nào khớp tổng điểm {} (rubricVersionId={}) -- lưu kết quả không kèm xếp loại.",
                totalScore, policy.getRubricVersionId()
            );
            return null;
        }
        if (matchingBands.size() > 1) {
            LOGGER.error(
                "Cấu hình dải điểm CHỒNG LẤN: tổng điểm {} khớp {} dải của rubricVersionId={}."
                    + " Lấy dải order nhỏ nhất ({}). Quản trị cần sửa lại dải.",
                totalScore, matchingBands.size(), policy.getRubricVersionId(),
                matchingBands.get(0).getCode()
            );
        }
        return matchingBands.get(0);
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

    /**
     * Sàn thang của rubric mà policy này dùng -- điểm cho một phần/bài không có gì để tính.
     *
     * <p>Trước 2026-08-11 những chỗ đó trả thẳng 0. Số 0 chỉ đúng khi thang bắt đầu từ 0; thang
     * 4-10 thì nó nằm dưới sàn và không dải xếp loại nào chứa được.
     *
     * <p>Không tra được rubric version thì lùi về 0, giữ hành vi cũ cho dữ liệu thiếu cấu hình.
     */
    private BigDecimal resolveScaleFloor(AssessmentPolicy policy) {
        if (policy.getRubricVersionId() == null) {
            return scaled(BigDecimal.ZERO);
        }
        return rubricVersionRepository.findById(policy.getRubricVersionId())
            .map(version -> version.getScoringScaleMin())
            .map(this::scaled)
            .orElseGet(() -> scaled(BigDecimal.ZERO));
    }

    /**
     * Chuẩn hoá điểm phần: {@code Σ(điểm_câu × trọng_số_câu) / Σ trọng_số_câu}.
     *
     * <p>Phép chia này THÊM 2026-08-11. Trước đó tầng câu→phần chỉ cộng tích mà không chia, trong
     * khi tầng phần→tổng ngay bên dưới lại có chia -- hai quy ước ngược nhau trong cùng một hàm.
     * Nó vô hại đúng một trường hợp: trọng số các câu trong phần cộng lại vừa bằng 1. Lệch khỏi đó
     * là điểm phần rời khỏi thang rubric, và khi tổng điểm rời thang thì KHÔNG dải xếp loại nào
     * khớp -- học sinh có điểm mà không có xếp loại.
     *
     * <p>Ví dụ đã đo: một phần có 2 câu, mỗi câu trọng số 1.00, hai câu được 83.47 và 74.78 trên
     * thang 100. Bản cũ cho điểm phần 158.25; bản này cho 79.13.
     *
     * <p>Hai mức lùi khi không chia được, cả hai đều cho ra điểm NẰM TRONG thang:
     *
     * <ul>
     *   <li>Mọi câu trong phần đều trọng số 0 -- trả trung bình cộng các câu. Đọc đúng ý "không ai
     *       khai trọng số thì các câu ngang nhau", chứ không phải "mọi câu đều vô giá trị": bản
     *       trước trả về tổng đã nhân, tức 0, và trên thang 4-10 thì 0 nằm ngoài thang.
     *   <li>Phần không có câu nào -- trả sàn thang, cũng vì lý do đó.
     * </ul>
     */
    private BigDecimal normalizedSectionScore(
            BigDecimal weightedSum,
            BigDecimal weightSum,
            BigDecimal plainSum,
            int itemCount,
            BigDecimal scaleFloor) {
        if (weightSum != null && weightSum.compareTo(BigDecimal.ZERO) > 0) {
            return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
        }
        if (itemCount > 0) {
            return plainSum.divide(BigDecimal.valueOf(itemCount), 2, RoundingMode.HALF_UP);
        }
        return scaleFloor;
    }

    private BigDecimal calculateTotalScore(
            List<AssessmentPaperSection> orderedSections,
            LinkedHashMap<UUID, BigDecimal> sectionScores,
            BigDecimal scaleFloor) {
        if (orderedSections.isEmpty()) {
            return scaleFloor;
        }

        BigDecimal totalWeight = orderedSections.stream()
            .map(section -> section.getWeight() == null ? BigDecimal.ZERO : section.getWeight())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal weightedTotal = orderedSections.stream()
                .map(section -> sectionScores.getOrDefault(section.getId(), scaleFloor)
                    .multiply(section.getWeight() == null ? BigDecimal.ZERO : section.getWeight()))
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
            return scaled(weightedTotal.divide(totalWeight, 2, RoundingMode.HALF_UP));
        }

        BigDecimal plainTotal = sectionScores.values().stream().reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        return scaled(plainTotal.divide(BigDecimal.valueOf(sectionScores.size()), 2, RoundingMode.HALF_UP));
    }

    private record LoadedSession(
        AssessmentSession session,
        UUID assessmentId,
        AssessmentPolicy policy,
        List<AssessmentPaperSection> orderedSections,
        List<AssessmentItemResponse> responses,
        Map<UUID, AssessmentPaperItem> paperItemsById,
        /** Sàn thang rubric -- điểm của một phần/bài không có gì để tính. */
        BigDecimal scaleFloor
    ) {
    }

    private record RolledUpScores(
        BigDecimal totalScore,
        List<SectionScore> sections,
        List<ItemScore> items
    ) {
    }

    public record CalculatedAssessmentSessionResult(
        UUID sessionId,
        UUID assessmentId,
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
    public record PreviewedAssessmentSessionResult(
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

    /**
     * @param questionId the question behind this item. This class does NOT load the question content -- scoring
     *                   does not need to read the question, and pulling the questions table in here would make every score
     *                   computation carry one more query that does not serve the computation. The display side
     *                   ({@code ViewAssessmentSessionResultUseCase}) loads the content in batches from this id.
     */
    public record ItemScore(
        UUID paperItemId,
        UUID responseId,
        UUID sectionId,
        UUID questionId,
        BigDecimal itemScore,
        BigDecimal weightedScore
    ) {
    }
}
