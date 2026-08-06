package com.sep.vox.application.port.input.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * ResolveNextPracticeQuestionUseCase cho các câu sau).
 *
 * <p>Thang leo còn BA bậc: đọc kho đúng tiêu chí -> đọc kho bỏ lọc tiêu chí -> nhờ LLM sinh mới.
 * Bậc "hàng xóm ngữ nghĩa (Chroma)" đã bỏ 2026-08-05 vì nó kéo câu của CHỦ ĐỀ KHÁC lên -- lý do
 * đầy đủ ghi ở {@link #ladderCandidatesForOneQuestion}.
 */
@Service
public class PracticeQuestionSelectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeQuestionSelectionService.class);
    private static final JsonMapper JSON_MAPPER = new JsonMapper();
    private static final int ONLINE_GENERATION_MAX_CANDIDATES = 2;

    /**
     * Lấy ngẫu nhiên trong {@value} câu tốt nhất thay vì luôn lấy câu đầu bảng -- randomesque
     * (Kingsbury &amp; Zara 1989). Chọn cứng câu đầu thì cùng một kho, cùng một trọng tâm sẽ
     * cho ra cùng một câu cho mọi học sinh, và câu đó bị dùng mòn trong khi phần còn lại của
     * kho không ai đụng tới.
     */
    private static final int TOP_CANDIDATES_BEFORE_RANDOM = 5;

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
        if (forcedSubAttribute != null) {
            var forcedCriterion = SubAttributePolicy.criterionForSubAttribute(forcedSubAttribute);
            if (forcedCriterion == null) {
                throw new IllegalArgumentException("Sub-attribute không thuộc taxonomy luyện tập.");
            }
            // Học sinh bấm "luyện lại lỗi này" -- ép đúng một tiêu chí, KHÔNG xen lẫn. Đây là
            // lựa chọn có chủ đích của người học, không phải chỗ để hệ thống tự cân đối.
            return new PracticeFocusInfo(
                List.of(forcedCriterion),
                Map.of(forcedCriterion, List.of(forcedSubAttribute))
            );
        }

        // Dùng CẢ danh sách xếp theo mức yếu, không cắt còn hai. Truy vấn vốn đã trả về đủ;
        // việc cắt trước đây khiến ba tiêu chí còn lại không bao giờ được hỏi tới.
        var criteria = weaknessRepository.findFocusCriterionCodesOrderedByWeakness(studentId);
        var ordered = criteria.isEmpty() ? List.of("GRAMMAR") : List.copyOf(criteria);

        var priorities = priorityRepository.findPracticeablePrioritiesOrderedDesc(studentId);
        var subAttributes = new LinkedHashMap<String, List<String>>();
        for (var criterion : ordered) {
            var forCriterion = subAttributesFor(priorities, criterion);
            if (!forCriterion.isEmpty()) {
                subAttributes.put(criterion, forCriterion);
            }
        }
        return new PracticeFocusInfo(ordered, Map.copyOf(subAttributes));
    }

    /**
     * Sub-attribute yếu nhất của tiêu chí, hoặc null khi chưa có dữ liệu điểm yếu.
     *
     * <p>null ở đây nghĩa là "luyện tiêu chí này nói chung", KHÔNG phải thiếu sót -- bịa ra
     * một sub-attribute khi chưa có bằng chứng là nói với học sinh rằng em yếu chỗ đó mà
     * không có cơ sở. Bên sinh câu phải chịu được null (xem CandidateFilterNode).
     *
     * <p>Nhánh null chỉ chạy ở giai đoạn đầu: sub_attribute_priority đòi frequency >= 3 nên
     * nhãn chỉ xuất hiện từ lần chấm thứ ba trở đi. Đo trên dữ liệu thật (2026-08-05, 4 lần
     * chấm): 4 nhãn đều có mặt và practiceable = true -- cột đó do WeaknessVectorCalculator
     * đặt theo "nhãn có thuộc taxonomy đóng không", tức có nhắm được khi sinh câu hay không.
     */
    private List<String> subAttributesFor(List<PracticeablePriority> priorities, String criterion) {
        return priorities.stream()
            .filter(entry -> entry.criterionCode().equals(criterion))
            .map(PracticeablePriority::subAttribute)
            .filter(value -> SubAttributePolicy.plannedSubAttribute(criterion, value) != null)
            .distinct()
            .toList();
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
        // Chu kỳ 4 ô: yếu nhất, yếu nhất, yếu nhì, xoay vòng phần còn lại -- xem
        // PracticeFocusInfo.criterionForSlot. Bản trước chỉ có hai tiêu chí và cắt cứng ở ô
        // thứ 2, nên từ ô thứ ba trở đi mọi câu đều nhắm đúng một tiêu chí.
        var criterion = focus.criterionForSlot(slotIndex);
        var subAttribute = SubAttributePolicy.plannedSubAttribute(
            criterion,
            focus.subAttributeForSlot(slotIndex)
        );
        // Trần bậc đọc từ framework đang áp, KHÔNG cứng 6: đổi trường sang thang khác (CEFR 6,
        // IELTS 9) thì hằng số 6 kẹp sai mà không báo lỗi. Lấy MỘT lần rồi truyền xuống thang
        // leo, tránh query lặp ở từng bậc.
        var bandCount = enrichmentService.frameworkBandCount(studentId);
        // Mọi ô cùng một bậc: bậc học sinh CHỌN. Trước đây ô thứ 4 trở đi tự nâng thêm một
        // bậc -- nghĩa là độ khó trôi ngay giữa phiên, theo một luật học sinh không nhìn thấy.
        var excludeIds = alreadyChosenInSession.stream().map(PracticeQuestion::getId).toList();

        // Hai bậc đọc DB trước (rẻ), CHƯA sinh mới.
        var candidates = ladderCandidatesForOneQuestion(
            topic, studentId, criterion, targetRank, excludeIds, bandCount
        );
        // Ba nước, đắt dần. Điều kiện đi tiếp là KHÔNG CHỌN ĐƯỢC CÂU DÙNG ĐƯỢC -- không phải
        // "thang leo không trả về dòng nào". Lẫn lộn hai thứ đó đã giết phiên luyện sau đúng
        // một câu: thang leo trả về 1 câu, pickOne loại nó vì trùng kiểu lập luận với câu vừa
        // hỏi (kho chủ đề có 12/14 câu cùng kiểu 'description'), thế là hết câu để hỏi trong
        // khi bậc sinh không bao giờ chạy vì "đã tìm được 1 dòng rồi".
        var chosen = pickOne(candidates, alreadyChosenInSession, subAttribute, targetRank, false);
        if (chosen.isEmpty()) {
            // Nới ĐÚNG luật đa dạng kiểu lập luận, GIỮ NGUYÊN luật gần-trùng-nội-dung. Hai luật
            // này không cùng hạng: hỏi hai câu cùng kiểu lập luận chỉ là kém phong phú, còn hỏi
            // lại một câu gần y hệt thì học sinh thấy rõ là bị lặp.
            chosen = pickOne(candidates, alreadyChosenInSession, subAttribute, targetRank, true);
        }
        if (chosen.isEmpty()) {
            // Chỉ tới đây mới trả giá LLM 10-40 giây với học sinh đang ngồi chờ -- khi kho thật
            // sự không còn gì hỏi được, chứ không phải mỗi lần pool chưa đủ 4 câu như bản gốc.
            var generated = generateThenReload(
                topic, studentId, criterion, subAttribute, targetRank, excludeIds, bandCount
            );
            chosen = pickOne(generated, alreadyChosenInSession, subAttribute, targetRank, true);
        }
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        chosen.ifPresent(question -> questionRepository.incrementUsageCount(question.getId()));
        return chosen.map(question -> new NextQuestionSelection(
            question, slotIndex + 1, criterion, subAttribute, targetRank
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
            .sorted(rankThenSubAttribute(subAttribute, targetRank))
            .limit(TOP_CANDIDATES_BEFORE_RANDOM)
            .toList();
        if (ranked.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ranked.get(ThreadLocalRandom.current().nextInt(ranked.size())));
    }

    /**
     * Hai khoá sắp xếp, KHÔNG trọng số nào: gần độ khó mong muốn nhất trước; bằng nhau thì câu
     * nhắm đúng nhãn điểm yếu trước.
     *
     * <p>Bản trước là tổng có trọng số bốn hạng {@code 0.40·zpd + 0.30·sub + 0.20·criterionMatch
     * + 0.10·fresh}. Hai hạng cuối không đổi được kết quả nào có ý nghĩa: {@code zpd} nhảy theo
     * bước 0,20 sau khi nhân trọng số, trong khi biên độ tối đa của {@code criterionMatch} là
     * 0,12 và của {@code fresh} là 0,10 -- không hạng nào một mình lật nổi một bước độ khó.
     * Chúng chỉ phá hoà giữa các câu đã bằng điểm, mà ngay dòng dưới {@code limit(5)} +
     * {@code ThreadLocalRandom} đã CỐ Ý chọn ngẫu nhiên trong nhóm hoà đó rồi.
     *
     * <p>{@code criterionMatch} còn gần như luôn bằng 1.0 nữa, vì bậc đầu của thang leo đã lọc
     * theo tiêu chí ngay trong SQL; và {@code fresh} gần như luôn bằng 1.0 ở quy mô hiện tại vì
     * {@code usage_count} hiếm khi bén mảng tới 50.
     */
    private Comparator<PracticeQuestion> rankThenSubAttribute(String subAttribute, int targetRank) {
        return Comparator
            .comparingInt((PracticeQuestion question) ->
                Math.abs(question.getDifficultyRank() - targetRank))
            .thenComparingInt(question -> subAttribute != null
                && subAttribute.equals(question.getTargetSubAttribute()) ? 0 : 1);
    }

    /**
     * HAI nước đọc kho, rộng dần -- trước là ba, xem lý do bỏ nước Chroma ở cuối hàm.
     *
     * <p>Bản trước nữa có bốn: nước đầu tìm ĐÚNG {@code rank = targetRank},
     * nước hai mới nới {@code ±1}. Gộp lại được vì phép sắp xếp
     * ({@link #rankThenSubAttribute}) đã tự đẩy câu đúng bậc lên đầu -- giữ hai truy vấn chỉ
     * khác nhau ở khoảng bậc là làm cùng một việc hai lần.
     */
    private List<PracticeQuestion> ladderCandidatesForOneQuestion(
            PracticeTopic topic,
            UUID studentId,
            String criterion,
            int targetRank,
            List<UUID> excludeIds,
            int bandCount) {
        var selected = new LinkedHashMap<UUID, PracticeQuestion>();
        var rankMin = Math.max(1, targetRank - 1);
        var rankMax = Math.min(bandCount, targetRank + 1);

        questions(topic.getId(), studentId, criterion, rankMin, rankMax)
            .forEach(question -> putIfNotExcluded(selected, question, excludeIds));

        if (selected.size() < generationProperties.paperTargetQuestionCount()) {
            questions(topic.getId(), studentId, null, rankMin, rankMax)
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
                enrichmentService.frameworkBandLadder(studentId),
                // Câu đã chết vĩnh viễn với CHÍNH học sinh này. Không gửi xuống thì cổng chặn
                // trùng bên Python so bản nháp mới với cả kho -- kể cả những câu em ấy không
                // bao giờ được thấy lại -- rồi vứt sạch vì "giống câu đã có", và chủ đề khoá
                // cứng ở pool_exhausted mãi mãi.
                questionRepository.findPermanentlyExhaustedIds(topic.getId(), studentId)
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
