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
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
public class ExamSessionResultCalculator {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(ExamSessionResultCalculator.class);

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;

    public ExamSessionResultCalculator(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            RubricResultBandRepository rubricResultBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricVersionRepository rubricVersionRepository) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
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
        // Tổng trọng số câu THEO TỪNG PHẦN -- mẫu số của phép chuẩn hoá bên dưới.
        var sectionWeightSums = new LinkedHashMap<UUID, BigDecimal>();
        // Tổng điểm câu KHÔNG nhân trọng số, và số câu -- chỉ dùng khi mọi trọng số trong phần đều
        // bằng 0, lúc đó lùi về trung bình cộng (xem normalizedSectionScore).
        var sectionPlainSums = new LinkedHashMap<UUID, BigDecimal>();
        var sectionItemCounts = new LinkedHashMap<UUID, Integer>();
        loaded.orderedSections().forEach(section -> {
            sectionScores.put(section.getId(), scaled(BigDecimal.ZERO));
            sectionWeightSums.put(section.getId(), BigDecimal.ZERO);
            sectionPlainSums.put(section.getId(), BigDecimal.ZERO);
            sectionItemCounts.put(section.getId(), 0);
        });

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

        var sections = loaded.orderedSections().stream()
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
        var paperItemsById = examPaperItemRepository.findByPaperId(session.getPaperId()).stream()
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
        var sectionRankById = new java.util.HashMap<UUID, Integer>();
        for (var index = 0; index < orderedSections.size(); index++) {
            sectionRankById.put(orderedSections.get(index).getId(), index);
        }
        var responses = examItemResponseRepository.findBySessionId(sessionId).stream()
            .sorted(Comparator
                .<ExamItemResponse>comparingInt(response -> {
                    var paperItem = paperItemsById.get(response.getPaperItemId());
                    return paperItem == null
                        ? Integer.MAX_VALUE
                        : sectionRankById.getOrDefault(paperItem.getSectionId(), Integer.MAX_VALUE);
                })
                .thenComparingInt(response -> {
                    var paperItem = paperItemsById.get(response.getPaperItemId());
                    return paperItem == null ? Integer.MAX_VALUE : paperItem.getOrder();
                })
                .thenComparing(response -> response.getId()))
            .toList();

        return new LoadedSession(
            session, exam.getId(), policy, orderedSections, responses, paperItemsById,
            resolveScaleFloor(policy));
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
        if (policy.getRubricVersionId() == null) {
            return null;
        }
        var matchingBands = rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
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
            List<ExamPaperSection> orderedSections,
            LinkedHashMap<UUID, BigDecimal> sectionScores,
            BigDecimal scaleFloor) {
        if (orderedSections.isEmpty()) {
            return scaleFloor;
        }

        var totalWeight = orderedSections.stream()
            .map(section -> section.getWeight() == null ? BigDecimal.ZERO : section.getWeight())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            var weightedTotal = orderedSections.stream()
                .map(section -> sectionScores.getOrDefault(section.getId(), scaleFloor)
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
        Map<UUID, ExamPaperItem> paperItemsById,
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

    /**
     * @param questionId câu hỏi đứng sau item này. Lớp này KHÔNG nạp nội dung câu hỏi -- chấm
     *                   điểm không cần đọc đề, và kéo bảng questions vào đây thì mỗi lần tính
     *                   điểm lại gánh thêm một truy vấn chẳng phục vụ việc tính. Bên hiển thị
     *                   ({@code ViewExamSessionResultUseCase}) tự nạp nội dung theo lô từ id này.
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
