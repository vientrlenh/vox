package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
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

import com.sep.vox.application.port.output.PracticeGenerationConfigPort;
import com.sep.vox.application.port.output.QuestionDiversityPort;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.CriterionScoreAverageRepository;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.domain.service.personalization.SubAttributePolicy;
import com.sep.vox.domain.service.personalization.TensePolicy;

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

    /**
     * Lấy ngẫu nhiên trong {@value} câu tốt nhất thay vì luôn lấy câu đầu bảng -- randomesque
     * (Kingsbury &amp; Zara 1989). Chọn cứng câu đầu thì cùng một kho, cùng một trọng tâm sẽ
     * cho ra cùng một câu cho mọi học sinh, và câu đó bị dùng mòn trong khi phần còn lại của
     * kho không ai đụng tới.
     */
    private static final int TOP_CANDIDATES_BEFORE_RANDOM = 5;

    /**
     * Năm tiêu chí của khung chấm, cố định (quyết định của người dùng: không tổng quát hoá
     * phần này). Dùng để BÙ cho những tiêu chí chưa có điểm nào -- xem {@link #resolveFocus}.
     *
     * <p>Thứ tự liệt kê ở đây là thứ tự dự phòng khi học sinh chưa được chấm gì: bắt đầu từ
     * ngữ pháp như hành vi cũ ({@code criteria.isEmpty() ? List.of("GRAMMAR")}), nhưng KHÔNG
     * dừng ở đó -- dừng ở đó chính là cái bẫy "mọi câu hỏi vĩnh viễn là ngữ pháp".
     */
    private static final List<String> ALL_PRACTICE_CRITERIA = List.of(
        "GRAMMAR", "VOCABULARY", "COHERENCE", "PRONUNCIATION", "FLUENCY"
    );

    private final PracticeQuestionRepository questionRepository;
    private final PracticeQuestionGenerationService generationService;
    private final QuestionDiversityPort diversityPort;
    private final PracticeGenerationConfigPort generationConfig;
    private final CriterionScoreAverageRepository criterionScoreRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public PracticeQuestionSelectionService(
            PracticeQuestionRepository questionRepository,
            PracticeQuestionGenerationService generationService,
            QuestionDiversityPort diversityPort,
            PracticeGenerationConfigPort generationConfig,
            CriterionScoreAverageRepository criterionScoreRepository,
            PracticeTopicOfferEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
        this.questionRepository = questionRepository;
        this.generationService = generationService;
        this.diversityPort = diversityPort;
        this.generationConfig = generationConfig;
        this.criterionScoreRepository = criterionScoreRepository;
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

        // Tiêu chí điểm THẤP nhất luyện trước. Nguồn là điểm đã chấm, không còn là hồ sơ điểm
        // yếu (đã gỡ khỏi hệ thống) -- xem CriterionScoreAverageRepository để biết đổi những gì.
        var scored = criterionScoreRepository.findCriterionCodesOrderedByLowestAverageScore(studentId);

        // BÙ cho đủ năm tiêu chí. Query chỉ trả về tiêu chí ĐÃ có điểm; nếu lấy nguyên danh sách
        // đó thì hai chỗ hỏng cùng lúc:
        //   1. học sinh chưa được chấm lần nào -> danh sách rỗng -> criterionForSlot ném
        //      IndexOutOfBounds ngay câu đầu tiên
        //   2. học sinh mới chấm đúng một tiêu chí -> chu kỳ 4 ô xoay quanh mỗi tiêu chí đó,
        //      nên bốn tiêu chí còn lại vĩnh viễn không có điểm mới, nên vĩnh viễn không lọt
        //      vào danh sách. Đúng kiểu bế tắc tự khoá đã gặp ở nhánh sub-attribute cũ.
        // Chưa đo được thì xếp SAU (không tuyên bố là yếu), nhưng phải có mặt để còn được hỏi tới.
        var ordered = new ArrayList<String>(scored);
        for (var criterion : ALL_PRACTICE_CRITERIA) {
            if (!ordered.contains(criterion)) {
                ordered.add(criterion);
            }
        }

        // Map rỗng = "luyện tiêu chí này nói chung" ở MỌI ô. Bảng sub_attribute_priority đã bị
        // xoá cùng hồ sơ điểm yếu, nên không còn nguồn nào nói được nhãn con nào đáng luyện.
        // PracticeFocusInfo.subAttributeForSlot trả null khi thiếu khoá, và cả Java lẫn Python
        // đều đã chịu được null từ trước (SubAttributePolicy.plannedSubAttribute,
        // CandidateFilterNode.rule_violations).
        //
        // Việc nhắm hẹp hơn tiêu chí giờ do target_tense đảm nhiệm -- xem §8 của
        // task/implement/ke-hoach-bo-diem-yeu-va-ep-thi.md.
        return new PracticeFocusInfo(List.copyOf(ordered), Map.of());
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
        var bandCount = enrichmentService.frameworkBandCount();
        // Thì đích của ô này: chủ đề nói trước (chủ đề lịch sử thì khoá quá khứ), chủ đề MIXED
        // thì xoay vòng theo ô để một buổi phủ nhiều khung thời gian. Bậc có quyền phủ quyết --
        // xem TensePolicy, thì và độ khó KHÔNG độc lập.
        var tense = TensePolicy.forSlot(
            topic.getTemporalAffordance(), slotIndex, targetRank, bandCount
        );
        // Mọi ô cùng một bậc: bậc học sinh CHỌN. Trước đây ô thứ 4 trở đi tự nâng thêm một
        // bậc -- nghĩa là độ khó trôi ngay giữa phiên, theo một luật học sinh không nhìn thấy.
        var excludeIds = alreadyChosenInSession.stream().map(question -> question.getId()).toList();

        // Hai bậc đọc DB trước (rẻ), CHƯA sinh mới.
        var candidates = ladderCandidatesForOneQuestion(
            topic, studentId, criterion, tense, targetRank, excludeIds, bandCount
        );
        // Ba nước, đắt dần. Điều kiện đi tiếp là KHÔNG CHỌN ĐƯỢC CÂU DÙNG ĐƯỢC -- không phải
        // "thang leo không trả về dòng nào". Lẫn lộn hai thứ đó đã giết phiên luyện sau đúng
        // một câu: thang leo trả về 1 câu, pickOne loại nó vì trùng kiểu lập luận với câu vừa
        // hỏi (kho chủ đề có 12/14 câu cùng kiểu 'description'), thế là hết câu để hỏi trong
        // khi bậc sinh không bao giờ chạy vì "đã tìm được 1 dòng rồi".
        var chosen = pickOne(candidates, alreadyChosenInSession, tense, targetRank, false);
        if (chosen.isEmpty()) {
            // Nới ĐÚNG luật đa dạng kiểu lập luận, GIỮ NGUYÊN luật gần-trùng-nội-dung. Hai luật
            // này không cùng hạng: hỏi hai câu cùng kiểu lập luận chỉ là kém phong phú, còn hỏi
            // lại một câu gần y hệt thì học sinh thấy rõ là bị lặp.
            chosen = pickOne(candidates, alreadyChosenInSession, tense, targetRank, true);
        }
        if (chosen.isEmpty()) {
            // Chỉ tới đây mới trả giá LLM 10-40 giây với học sinh đang ngồi chờ -- khi kho thật
            // sự không còn gì hỏi được, chứ không phải mỗi lần pool chưa đủ 4 câu như bản gốc.
            var generated = generateThenReload(
                topic, studentId, criterion, subAttribute, tense, targetRank, excludeIds, bandCount
            );
            chosen = pickOne(generated, alreadyChosenInSession, tense, targetRank, true);
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
            String tense,
            int targetRank,
            boolean relaxReasoning) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        var similarities = diversityPort.maxSimilarities(
            candidates.stream().map(question -> question.getId()).toList(),
            alreadyChosen.stream().map(question -> question.getId()).toList()
        );
        var previousReasoning = alreadyChosen.isEmpty() ? null : reasoningType(alreadyChosen.getLast());
        var ranked = candidates.stream()
            .filter(question -> alreadyChosen.isEmpty()
                || similarities.getOrDefault(question.getId(), 1.0) < 0.85)
            .filter(question -> relaxReasoning
                || previousReasoning == null
                || !previousReasoning.equals(reasoningType(question)))
            .sorted(rankThenTense(tense, targetRank))
            .limit(TOP_CANDIDATES_BEFORE_RANDOM)
            .toList();
        if (ranked.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ranked.get(ThreadLocalRandom.current().nextInt(ranked.size())));
    }

    /**
     * Hai khoá sắp xếp, KHÔNG trọng số nào: gần độ khó mong muốn nhất trước; bằng nhau thì câu
     * ép ĐÚNG thì đang cần trước.
     *
     * <p>Khoá phụ trước đây là "nhắm đúng nhãn điểm yếu". Nhãn đó đã chết: bảng
     * {@code sub_attribute_priority} bị gỡ nên {@code subAttribute} luôn null, tức khoá phụ
     * luôn trả 1 cho mọi câu và không phá hoà được gì. Thì thay đúng vai đó -- và nó phân biệt
     * được thật, vì thang leo cố ý nhận cả câu {@code target_tense IS NULL}.
     *
     * <p>Bản trước nữa là tổng có trọng số bốn hạng {@code 0.40·zpd + 0.30·sub
     * + 0.20·criterionMatch + 0.10·fresh}. Hai hạng cuối không đổi được kết quả nào có ý nghĩa:
     * {@code zpd} nhảy theo bước 0,20 sau khi nhân trọng số, trong khi biên độ tối đa của
     * {@code criterionMatch} là 0,12 và của {@code fresh} là 0,10 -- không hạng nào một mình
     * lật nổi một bước độ khó. Chúng chỉ phá hoà giữa các câu đã bằng điểm, mà ngay dòng dưới
     * {@code limit(5)} + {@code ThreadLocalRandom} đã CỐ Ý chọn ngẫu nhiên trong nhóm hoà đó rồi.
     */
    private Comparator<PracticeQuestion> rankThenTense(String tense, int targetRank) {
        return Comparator
            .comparingInt((PracticeQuestion question) ->
                Math.abs(question.getDifficultyRank() - targetRank))
            .thenComparingInt(question -> tense != null
                && tense.equals(question.getTargetTense()) ? 0 : 1);
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
            String tense,
            int targetRank,
            List<UUID> excludeIds,
            int bandCount) {
        var selected = new LinkedHashMap<UUID, PracticeQuestion>();
        var rankMin = Math.max(1, targetRank - 1);
        var rankMax = Math.min(bandCount, targetRank + 1);

        questions(topic.getId(), studentId, criterion, tense, rankMin, rankMax)
            .forEach(question -> putIfNotExcluded(selected, question, excludeIds));

        // Nới THÌ trước, nới tiêu chí sau. Thứ tự này không tuỳ tiện: hỏi hai câu cùng khung
        // thời gian chỉ là kém phong phú, còn hỏi câu nhắm sai tiêu chí thì ô đó không luyện
        // đúng thứ đang cần luyện.
        if (selected.size() < generationConfig.paperTargetQuestionCount()) {
            questions(topic.getId(), studentId, criterion, null, rankMin, rankMax)
                .forEach(question -> putIfNotExcluded(selected, question, excludeIds));
        }

        if (selected.size() < generationConfig.paperTargetQuestionCount()) {
            questions(topic.getId(), studentId, null, null, rankMin, rankMax)
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
            String tense,
            int targetRank,
            List<UUID> excludeIds,
            int bandCount) {
        var selected = new LinkedHashMap<UUID, PracticeQuestion>();
        try {
            generationService.generateAndStore(
                topic.getId(),
                criterion,
                subAttribute,
                tense,
                targetRank,
                generationConfig.onlineCandidateCount(),
                generationConfig.onlineBudget(),
                bandCount,
                enrichmentService.frameworkBandLadder(),
                // Câu đã chết vĩnh viễn với CHÍNH học sinh này. Không gửi xuống thì cổng chặn
                // trùng bên Python so bản nháp mới với cả kho -- kể cả những câu em ấy không
                // bao giờ được thấy lại -- rồi vứt sạch vì "giống câu đã có", và chủ đề khoá
                // cứng ở pool_exhausted mãi mãi.
                questionRepository.findPermanentlyExhaustedIds(topic.getId(), studentId)
            );
            // Đọc lại KHÔNG lọc theo thì: vừa nhờ sinh đúng thì đó rồi, nhưng nếu evaluator
            // loại bản nháp vì mốc thời gian chưa ép được thì đích thì lô mới có thể mang thì
            // khác -- lọc tiếp ở đây là vứt luôn thứ vừa trả tiền để sinh ra.
            questions(
                topic.getId(),
                studentId,
                criterion,
                null,
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
            String tense,
            int rankMin,
            int rankMax) {
        return questionRepository.findUnseenByTopicAndCriterionAndRankRange(
            topicId,
            studentId,
            criterion,
            tense,
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
