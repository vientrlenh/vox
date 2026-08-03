package com.sep.vox.application.port.input.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.application.config.PracticeGenerationProperties;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.personalization.LearnerWeaknessSnapshotRepository;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.domain.repository.personalization.SubAttributePriorityRepository;
import com.sep.vox.domain.repository.personalization.SubAttributePriorityRepository.PracticeablePriority;
import com.sep.vox.domain.service.personalization.SubAttributePolicy;
import com.sep.vox.infrastructure.service.QuestionDiversityClient;

/**
 * Chọn/sinh ĐÚNG 1 câu MAIN tiếp theo trong lúc phiên luyện đang chạy -- tách từ
 * BuildPracticePaperUseCase vì giờ có 2 caller (buildPracticePaper cho câu đầu tiên,
 * ResolveNextPracticeQuestionUseCase cho các câu sau). Bậc 1->2->3->4 giữ nguyên logic
 * (đọc kho, nới rank, Chroma neighbor, LLM sinh mới), chỉ đổi từ "gom N câu cho cả đề"
 * sang "gom candidate rồi chọn đúng 1 câu, loại trừ những câu đã chọn trong phiên".
 */
@Service
public class PracticeQuestionSelectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeQuestionSelectionService.class);
    private static final JsonMapper JSON_MAPPER = new JsonMapper();
    private static final int ONLINE_GENERATION_MAX_CANDIDATES = 2;

    private final PracticeQuestionRepository questionRepository;
    private final PracticeQuestionGenerationService generationService;
    private final QuestionDiversityClient diversityClient;
    private final PracticeGenerationProperties generationProperties;
    private final LearnerWeaknessSnapshotRepository weaknessRepository;
    private final SubAttributePriorityRepository priorityRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public PracticeQuestionSelectionService(
            PracticeQuestionRepository questionRepository,
            PracticeQuestionGenerationService generationService,
            QuestionDiversityClient diversityClient,
            PracticeGenerationProperties generationProperties,
            LearnerWeaknessSnapshotRepository weaknessRepository,
            SubAttributePriorityRepository priorityRepository,
            PracticeTopicOfferEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
        this.questionRepository = questionRepository;
        this.generationService = generationService;
        this.diversityClient = diversityClient;
        this.generationProperties = generationProperties;
        this.weaknessRepository = weaknessRepository;
        this.priorityRepository = priorityRepository;
    }

    /**
     * Tiêu chí/sub-attribute trọng tâm của học sinh -- dùng chung bởi BuildPracticePaperUseCase
     * (câu đầu tiên) và ResolveNextPracticeQuestionUseCase (các câu sau trong phiên).
     */
    public PracticeFocusInfo resolveFocus(UUID studentId, String forcedSubAttribute) {
        var criteria = weaknessRepository.findFocusCriterionCodesOrderedByWeakness(studentId);
        var primary = criteria.isEmpty() ? "GRAMMAR" : criteria.get(0);
        var secondary = criteria.size() < 2 ? primary : criteria.get(1);
        if (forcedSubAttribute != null) {
            var forcedCriterion = SubAttributePolicy.criterionForSubAttribute(forcedSubAttribute);
            if (forcedCriterion == null) {
                throw new IllegalArgumentException("Sub-attribute không thuộc taxonomy luyện tập.");
            }
            return new PracticeFocusInfo(forcedCriterion, forcedCriterion, forcedSubAttribute, forcedSubAttribute);
        }
        var priorities = priorityRepository.findPracticeablePrioritiesOrderedDesc(studentId);
        return new PracticeFocusInfo(
            primary,
            secondary,
            firstSubAttribute(priorities, primary),
            firstSubAttribute(priorities, secondary)
        );
    }

    /**
     * Sub-attribute yếu nhất của tiêu chí, hoặc null khi chưa có dữ liệu điểm yếu.
     *
     * <p>null ở đây nghĩa là "luyện tiêu chí này nói chung", KHÔNG phải thiếu sót -- bịa ra
     * một sub-attribute khi chưa có bằng chứng là nói với học sinh rằng em yếu chỗ đó mà
     * không có cơ sở. Bên sinh câu phải chịu được null (xem CandidateFilterNode).
     *
     * <p>Hiện nhánh null gần như luôn chạy, vì findPracticeablePrioritiesOrderedDesc lọc
     * practiceable = true mà cột đó chưa có chỗ nào set true.
     */
    private String firstSubAttribute(List<PracticeablePriority> priorities, String criterion) {
        return priorities.stream()
            .filter(entry -> entry.criterionCode().equals(criterion))
            .map(PracticeablePriority::subAttribute)
            .filter(value -> SubAttributePolicy.plannedSubAttribute(criterion, value) != null)
            .findFirst()
            .orElse(null);
    }

    /** Câu đã chọn + toàn bộ metadata slot đi kèm -- để caller ghi PracticePaperItem không phải tự suy lại tiêu chí/rank theo slot. */
    public record NextQuestionSelection(
        PracticeQuestion question, int slot, String criterion, String subAttribute, int targetRank) {
    }

    public Optional<NextQuestionSelection> resolveNextQuestion(
            PracticeTopic topic,
            UUID studentId,
            PracticeFocusInfo focus,
            int targetRank,
            List<PracticeQuestion> alreadyChosenInSession) {
        var slotIndex = alreadyChosenInSession.size();
        var criterion = slotIndex < 2 ? focus.primaryCriterion() : focus.secondaryCriterion();
        var subAttribute = SubAttributePolicy.plannedSubAttribute(
            criterion,
            slotIndex < 2 ? focus.primarySubAttribute() : focus.secondarySubAttribute()
        );
        // Trần bậc đọc từ framework đang áp, KHÔNG cứng 6: đổi trường sang thang khác (CEFR 6,
        // IELTS 9) thì hằng số 6 kẹp sai mà không báo lỗi. Lấy MỘT lần rồi truyền xuống thang
        // leo, tránh query lặp ở từng bậc.
        var bandCount = enrichmentService.frameworkBandCount(studentId);
        var slotRank = Math.min(bandCount, targetRank + (slotIndex >= 3 ? 1 : 0));
        var excludeIds = alreadyChosenInSession.stream().map(PracticeQuestion::getId).toList();

        // Ba bậc đọc DB trước (rẻ), CHƯA sinh mới.
        var candidates = ladderCandidatesForOneQuestion(
            topic, studentId, criterion, subAttribute, slotRank, excludeIds, bandCount
        );
        // Ba nước, đắt dần. Điều kiện đi tiếp là KHÔNG CHỌN ĐƯỢC CÂU DÙNG ĐƯỢC -- không phải
        // "thang leo không trả về dòng nào". Lẫn lộn hai thứ đó đã giết phiên luyện sau đúng
        // một câu: thang leo trả về 1 câu, pickOne loại nó vì trùng kiểu lập luận với câu vừa
        // hỏi (kho chủ đề có 12/14 câu cùng kiểu 'description'), thế là hết câu để hỏi trong
        // khi bậc sinh không bao giờ chạy vì "đã tìm được 1 dòng rồi".
        var chosen = pickOne(candidates, alreadyChosenInSession, criterion, subAttribute, slotRank, false);
        if (chosen.isEmpty()) {
            // Nới ĐÚNG luật đa dạng kiểu lập luận, GIỮ NGUYÊN luật gần-trùng-nội-dung. Hai luật
            // này không cùng hạng: hỏi hai câu cùng kiểu lập luận chỉ là kém phong phú, còn hỏi
            // lại một câu gần y hệt thì học sinh thấy rõ là bị lặp.
            chosen = pickOne(candidates, alreadyChosenInSession, criterion, subAttribute, slotRank, true);
        }
        if (chosen.isEmpty()) {
            // Chỉ tới đây mới trả giá LLM 10-40 giây với học sinh đang ngồi chờ -- khi kho thật
            // sự không còn gì hỏi được, chứ không phải mỗi lần pool chưa đủ 4 câu như bản gốc.
            var generated = generateThenReload(
                topic, studentId, criterion, subAttribute, slotRank, excludeIds, bandCount
            );
            chosen = pickOne(generated, alreadyChosenInSession, criterion, subAttribute, slotRank, true);
        }
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        chosen.ifPresent(question -> questionRepository.incrementUsageCount(question.getId()));
        return chosen.map(question -> new NextQuestionSelection(
            question, slotIndex + 1, criterion, subAttribute, slotRank
        ));
    }

    /**
     * @param relaxReasoning bỏ luật "không hỏi hai câu liền cùng kiểu lập luận". Chỉ bật ở nước
     *     dự phòng của {@link #resolveNextQuestion}: luật đa dạng là thứ tốt-nếu-có, để nó kết
     *     thúc buổi luyện khi hạn mức vẫn còn thì hại hơn nhiều so với hai câu hơi giống nhau.
     *     Luật gần-trùng-nội-dung (0.85) KHÔNG bao giờ được nới -- hỏi lại một câu gần y hệt là
     *     thứ học sinh nhận ra ngay.
     */
    private Optional<PracticeQuestion> pickOne(
            List<PracticeQuestion> candidates,
            List<PracticeQuestion> alreadyChosen,
            String criterion,
            String subAttribute,
            int targetRank,
            boolean relaxReasoning) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        var similarities = diversityClient.maxSimilarities(
            candidates.stream().map(PracticeQuestion::getId).toList(),
            alreadyChosen.stream().map(PracticeQuestion::getId).toList()
        );
        var previousReasoning = alreadyChosen.isEmpty() ? null : reasoningType(alreadyChosen.getLast());
        var ranked = candidates.stream()
            .filter(question -> alreadyChosen.isEmpty()
                || similarities.getOrDefault(question.getId(), 1.0) < 0.85)
            .filter(question -> relaxReasoning
                || previousReasoning == null
                || !previousReasoning.equals(reasoningType(question)))
            .sorted(Comparator.comparingDouble(
                (PracticeQuestion question) -> quality(question, criterion, subAttribute, targetRank)
            ).reversed())
            .limit(5)
            .toList();
        if (ranked.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ranked.get(ThreadLocalRandom.current().nextInt(ranked.size())));
    }

    private double quality(
            PracticeQuestion question,
            String criterion,
            String subAttribute,
            int targetRank) {
        var zpd = Math.max(0.0, 1 - Math.abs(question.getDifficultyRank() - targetRank) / 2.0);
        var sub = subAttribute != null && subAttribute.equals(question.getTargetSubAttribute())
            ? 1.0
            : 0.3;
        var criterionMatch = criterion.equals(question.getTargetCriterionCode()) ? 1.0 : 0.4;
        var fresh = 1 - Math.min(1.0, question.getUsageCount() / 50.0);
        return 0.40 * zpd + 0.30 * sub + 0.20 * criterionMatch + 0.10 * fresh;
    }

    private List<PracticeQuestion> ladderCandidatesForOneQuestion(
            PracticeTopic topic,
            UUID studentId,
            String criterion,
            String subAttribute,
            int targetRank,
            List<UUID> excludeIds,
            int bandCount) {
        var selected = new LinkedHashMap<UUID, PracticeQuestion>();

        questions(topic.getId(), studentId, criterion, targetRank, targetRank)
            .forEach(question -> putIfNotExcluded(selected, question, excludeIds));

        if (selected.size() < generationProperties.paperTargetQuestionCount()) {
            questions(
                topic.getId(),
                studentId,
                null,
                Math.max(1, targetRank - 1),
                Math.min(bandCount, targetRank + 1)
            ).forEach(question -> putIfNotExcluded(selected, question, excludeIds));
        }

        if (selected.size() < generationProperties.paperTargetQuestionCount()) {
            var neighborIds = diversityClient.neighborQuestionIds(
                topic.getName(),
                criterion,
                Math.max(1, targetRank - 1),
                Math.min(bandCount, targetRank + 1)
            );
            questionRepository.findUnseenByIds(neighborIds, studentId)
                .forEach(question -> putIfNotExcluded(selected, question, excludeIds));
        }

        return List.copyOf(selected.values());
    }

    /**
     * Bậc 4: nhờ LLM sinh câu mới rồi đọc lại kho. Tách khỏi ba bậc đọc DB ở trên vì điều kiện
     * kích hoạt khác hẳn -- xem {@link #resolveNextQuestion}.
     */
    private List<PracticeQuestion> generateThenReload(
            PracticeTopic topic,
            UUID studentId,
            String criterion,
            String subAttribute,
            int targetRank,
            List<UUID> excludeIds,
            int bandCount) {
        var selected = new LinkedHashMap<UUID, PracticeQuestion>();
        try {
            generationService.generateAndStore(
                topic.getId(),
                criterion,
                subAttribute,
                targetRank,
                ONLINE_GENERATION_MAX_CANDIDATES,
                generationProperties.onlineBudget(),
                bandCount,
                enrichmentService.frameworkBandLadder(studentId)
            );
            questions(
                topic.getId(),
                studentId,
                criterion,
                Math.max(1, targetRank - 1),
                Math.min(bandCount, targetRank + 1)
            ).forEach(question -> putIfNotExcluded(selected, question, excludeIds));
        } catch (RuntimeException exception) {
            LOGGER.warn("Hết bậc sinh trực tiếp cho câu tiếp theo trong phiên.", exception);
        }
        return List.copyOf(selected.values());
    }

    private static void putIfNotExcluded(
            LinkedHashMap<UUID, PracticeQuestion> selected, PracticeQuestion question, List<UUID> excludeIds) {
        if (!excludeIds.contains(question.getId())) {
            selected.putIfAbsent(question.getId(), question);
        }
    }

    private List<PracticeQuestion> questions(
            UUID topicId,
            UUID studentId,
            String criterion,
            int rankMin,
            int rankMax) {
        return questionRepository.findUnseenByTopicAndCriterionAndRankRange(
            topicId,
            studentId,
            criterion,
            rankMin,
            rankMax
        );
    }

    private String reasoningType(PracticeQuestion question) {
        var value = question.getDifficultyFeaturesJson();
        if (value == null || value.isBlank()) {
            return null;
        }
        return JSON_MAPPER.readTree(value).path("reasoning_type").asString(null);
    }
}
