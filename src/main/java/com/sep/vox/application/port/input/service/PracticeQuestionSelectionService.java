package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.JsonSerializationPort;
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
@RequiredArgsConstructor
@Slf4j
public class PracticeQuestionSelectionService {

    /**
     * Pick at random among the {@value} best questions instead of always taking the top one -- randomesque
     * (Kingsbury &amp; Zara 1989). Always taking the top one would give the same bank and the same focus
     * the same question for every student, and that question would be worn out while the rest of the
     * bank is never touched.
     */
    private static final int TOP_CANDIDATES_BEFORE_RANDOM = 5;

    /**
     * The five criteria of the grading framework, fixed (user decision: this part is not
     * generalized). Used to COMPENSATE for criteria that have no score yet -- see {@link #resolveFocus}.
     *
     * <p>The listing order here is the fallback order when the student has not been graded on anything: start from
     * grammar as the old behavior did ({@code criteria.isEmpty() ? List.of("GRAMMAR")}), but do NOT
     * stop there -- stopping there is exactly the trap "every question is grammar forever".
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
    private final JsonSerializationPort jsonSerializationPort;

    /**
     * The student's focus criterion/sub-attribute -- shared by BuildPracticePaperUseCase
     * (the first question) and ResolveNextPracticeQuestionUseCase (the later questions in the session).
     *
     * <p>An empty Map = "practise this criterion in general" in EVERY slot. The sub_attribute_priority table was
     * removed together with the weakness profile, so no source is left that can say which sub-label deserves
     * practice. PracticeFocusInfo.subAttributeForSlot returns null when the key is missing, and both Java and
     * Python already tolerated null before (SubAttributePolicy.plannedSubAttribute,
     * CandidateFilterNode.rule_violations).
     *
     * <p>Narrowing beyond the criterion is now the job of target_tense -- see §8 of
     * task/implement/ke-hoach-bo-diem-yeu-va-ep-thi.md.
     */
    public PracticeFocusInfo resolveFocus(UUID studentId, String forcedSubAttribute) {
        if (forcedSubAttribute != null) {
            return forcedFocus(forcedSubAttribute);
        }

        // The LOWEST-scoring criterion is practised first. The source is the graded scores, no longer the weakness
        // profile (removed from the system) -- see CriterionScoreAverageRepository for what changed.
        List<String> scored = criterionScoreRepository.findCriterionCodesOrderedByLowestAverageScore(studentId);
        return new PracticeFocusInfo(List.copyOf(withUnscoredCriteriaAppended(scored)), Map.of());
    }

    private PracticeFocusInfo forcedFocus(String forcedSubAttribute) {
        String forcedCriterion = SubAttributePolicy.criterionForSubAttribute(forcedSubAttribute);
        if (forcedCriterion == null) {
            throw new IllegalArgumentException("Sub-attribute không thuộc taxonomy luyện tập.");
        }
        // The student pressed "practise this mistake again" -- force exactly one criterion, with NO mixing. This is a
        // deliberate choice of the learner, not a place for the system to balance things on its own.
        return new PracticeFocusInfo(
            List.of(forcedCriterion),
            Map.of(forcedCriterion, List.of(forcedSubAttribute))
        );
    }

    /**
     * COMPENSATES so that all five criteria are present. The query only returns criteria that ALREADY have a score;
     * taking that list as is breaks two things at once:
     * <ol>
     *   <li>a student who has never been graded -> empty list -> criterionForSlot throws IndexOutOfBounds on
     *       the very first question</li>
     *   <li>a student who has been graded on exactly one criterion -> the 4-slot cycle revolves around that one
     *       criterion, so the other four never get a new score, so they never enter the list. The same
     *       self-locking deadlock seen in the old sub-attribute branch.</li>
     * </ol>
     * A criterion not yet measured goes AFTER (it is not declared weak), but must be present so it still gets asked about.
     */
    private List<String> withUnscoredCriteriaAppended(List<String> scored) {
        List<String> ordered = new ArrayList<String>(scored);
        for (String criterion : ALL_PRACTICE_CRITERIA) {
            if (!ordered.contains(criterion)) {
                ordered.add(criterion);
            }
        }
        return ordered;
    }

    /** Câu đã chọn + toàn bộ metadata slot đi kèm -- để caller ghi PracticePaperItem không phải tự suy lại tiêu chí/rank theo slot. */
    public record NextQuestionSelection(
        PracticeQuestion question, int slot, String criterion, String subAttribute, int targetRank) {
    }

    /**
     * 4-slot cycle: weakest, weakest, second weakest, then rotate over the rest -- see
     * PracticeFocusInfo.criterionForSlot. The previous version had only two criteria and cut hard at the 2nd slot,
     * so from the third slot on every question aimed at exactly one criterion.
     *
     * <p>The band ceiling ({@code bandCount}) is passed down by the CALLER, taken from the framework of the very band the
     * student chose -- the "currently effective framework" is no longer looked up here. Since the practice screen lets the
     * student pick a framework, two students may be on frameworks with different band counts (6 for KNLNNVN/CEFR,
     * 9 for IELTS), and the band count goes straight into the difficulty clamp below and into TensePolicy.
     *
     * <p>The target tense of this slot: speak first about the topic (a historical topic locks the past), a MIXED topic
     * rotates by slot so one session covers several time frames. The band has veto power -- see
     * TensePolicy, tense and difficulty are NOT independent.
     *
     * <p>Every slot uses the same band: the band the student CHOSE. Previously from the 4th slot on the band was raised
     * by one by itself -- meaning difficulty drifted in the middle of a session, by a rule the student cannot see.
     */
    public Optional<NextQuestionSelection> resolveNextQuestion(
            PracticeTopic topic, UUID studentId, PracticeFocusInfo focus, int targetRank, int bandCount,
            UUID frameworkVersionId, List<PracticeQuestion> alreadyChosenInSession) {
        int slotIndex = alreadyChosenInSession.size();
        String criterion = focus.criterionForSlot(slotIndex);
        String subAttribute = SubAttributePolicy.plannedSubAttribute(criterion, focus.subAttributeForSlot(slotIndex));
        String tense = TensePolicy.forSlot(topic.getTemporalAffordance(), slotIndex, targetRank, bandCount);
        List<UUID> excludeIds = alreadyChosenInSession.stream().map(question -> question.getId()).toList();

        Optional<PracticeQuestion> chosen = chooseWithFallbacks(
            topic, studentId, criterion, subAttribute, tense, targetRank, bandCount,
            frameworkVersionId, alreadyChosenInSession, excludeIds
        );
        chosen.ifPresent(question -> questionRepository.incrementUsageCount(question.getId()));
        return chosen.map(question -> new NextQuestionSelection(
            question, slotIndex + 1, criterion, subAttribute, targetRank
        ));
    }

    /**
     * Two DB-reading rungs first (cheap), NOT generating anything new yet.
     *
     * <p>Three moves, increasingly expensive. The condition to go on is "COULD NOT PICK A USABLE QUESTION" -- not
     * "the ladder returned no row". Mixing the two up killed a practice session after exactly one question: the
     * ladder returned 1 question, pickOne dropped it because it repeated the reasoning type of the question just asked
     * (the topic bank has 12 of 14 questions of the same 'description' type), and then there was nothing left to ask
     * while the generating rung never ran because "a row was already found".
     *
     * <p>The second move relaxes EXACTLY the reasoning-type diversity rule and KEEPS the near-duplicate-content rule.
     * The two rules are not of the same rank: asking two questions of the same reasoning type is only less varied,
     * while asking again a nearly identical question is plainly a repeat to the student.
     *
     * <p>The third move pays the LLM cost of 10-40 seconds with the student sitting and waiting only here -- when the bank
     * truly has nothing left to ask, not every time the pool has fewer than 4 questions as in the original version.
     */
    private Optional<PracticeQuestion> chooseWithFallbacks(
            PracticeTopic topic, UUID studentId, String criterion, String subAttribute, String tense,
            int targetRank, int bandCount, UUID frameworkVersionId,
            List<PracticeQuestion> alreadyChosenInSession, List<UUID> excludeIds) {
        List<PracticeQuestion> candidates = ladderCandidatesForOneQuestion(
            topic, studentId, criterion, tense, targetRank, excludeIds, bandCount
        );
        Optional<PracticeQuestion> chosen = pickOne(candidates, alreadyChosenInSession, tense, targetRank, false);
        if (chosen.isEmpty()) {
            chosen = pickOne(candidates, alreadyChosenInSession, tense, targetRank, true);
        }
        if (chosen.isEmpty()) {
            List<PracticeQuestion> generated = generateThenReload(
                topic, studentId, criterion, subAttribute, tense, targetRank, excludeIds,
                bandCount, frameworkVersionId
            );
            chosen = pickOne(generated, alreadyChosenInSession, tense, targetRank, true);
        }
        return chosen;
    }

    /**
     * @param relaxReasoning drops the rule "do not ask two consecutive questions of the same reasoning type". Only turned on
     *     at the fallback move of {@link #resolveNextQuestion}: the diversity rule is nice-to-have, and letting it
     *     end the practice session while quota remains does far more harm than two slightly similar questions.
     *     The near-duplicate-content rule (0.85) must NEVER be relaxed -- asking again a nearly identical question is
     *     something the student notices immediately.
     */
    private Optional<PracticeQuestion> pickOne(
            List<PracticeQuestion> candidates, List<PracticeQuestion> alreadyChosen,
            String tense, int targetRank, boolean relaxReasoning) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Map<UUID, Double> similarities = diversityPort.maxSimilarities(
            candidates.stream().map(question -> question.getId()).toList(),
            alreadyChosen.stream().map(question -> question.getId()).toList()
        );
        String previousReasoning = alreadyChosen.isEmpty() ? null : reasoningType(alreadyChosen.getLast());
        List<PracticeQuestion> ranked = candidates.stream()
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
     * TWO moves of reading the bank, widening step by step -- it used to be three, see the reason for dropping the Chroma move at the end of the method.
     *
     * <p>The version before that had four: the first move looked for EXACTLY {@code rank = targetRank},
     * the second widened to {@code ±1}. They merge because the sort
     * ({@link #rankThenSubAttribute}) already pushes questions of the exact band to the top -- keeping two queries that
     * differ only in the band range does the same job twice.
     *
     * <p>Relax the TENSE first, the criterion second. This order is not arbitrary: asking two questions of the same
     * time frame is only less varied, while asking a question that aims at the wrong criterion means that slot does not
     * practise what needs practising.
     */
    private List<PracticeQuestion> ladderCandidatesForOneQuestion(
            PracticeTopic topic, UUID studentId, String criterion, String tense, int targetRank,
            List<UUID> excludeIds, int bandCount) {
        LinkedHashMap<UUID, PracticeQuestion> selected = new LinkedHashMap<UUID, PracticeQuestion>();
        int rankMin = Math.max(1, targetRank - 1);
        int rankMax = Math.min(bandCount, targetRank + 1);

        questions(topic.getId(), studentId, criterion, tense, rankMin, rankMax)
            .forEach(question -> putIfNotExcluded(selected, question, excludeIds));

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
     * Rung 4: ask the LLM to generate new questions, then read the bank again. Kept apart from the three DB-reading rungs above
     * because its trigger condition is entirely different -- see {@link #resolveNextQuestion}.
     */
    private List<PracticeQuestion> generateThenReload(
            PracticeTopic topic, UUID studentId, String criterion, String subAttribute, String tense,
            int targetRank, List<UUID> excludeIds, int bandCount, UUID frameworkVersionId) {
        LinkedHashMap<UUID, PracticeQuestion> selected = new LinkedHashMap<UUID, PracticeQuestion>();
        try {
            generateQuestions(topic, studentId, criterion, subAttribute, tense, targetRank, bandCount, frameworkVersionId);
            // Reload WITHOUT filtering by tense: we just asked for exactly that tense, but if the evaluator
            // rejects a draft because the time anchor could not be enforced, the new batch may carry a different
            // tense -- filtering again here would throw away what we just paid to generate.
            questions(
                topic.getId(),
                studentId,
                criterion,
                null,
                Math.max(1, targetRank - 1),
                Math.min(bandCount, targetRank + 1)
            ).forEach(question -> putIfNotExcluded(selected, question, excludeIds));
        } catch (RuntimeException exception) {
            log.warn("Hết bậc sinh trực tiếp cho câu tiếp theo trong phiên.", exception);
        }
        return List.copyOf(selected.values());
    }

    /**
     * The band ladder is the one of the EXACT framework the student is practising. Taking the no-argument frameworkBandLadder()
     * takes the framework currently effective system-wide -- the question-generation prompt would describe a different scale
     * from the one the student chose, and no error would be reported.
     *
     * <p>The excluded ids are the questions already dead for good for THIS very student. If they were not sent down, the
     * duplicate gate on the Python side would compare the new draft against the whole bank -- including questions the
     * student will never see again -- then throw everything away as "similar to an existing question", and the topic would be
     * locked at pool_exhausted forever.
     */
    private void generateQuestions(
            PracticeTopic topic, UUID studentId, String criterion, String subAttribute, String tense,
            int targetRank, int bandCount, UUID frameworkVersionId) {
        generationService.generateAndStore(
            topic.getId(),
            criterion,
            subAttribute,
            tense,
            targetRank,
            generationConfig.onlineCandidateCount(),
            generationConfig.onlineBudget(),
            bandCount,
            frameworkVersionId == null
                ? enrichmentService.frameworkBandLadder()
                : enrichmentService.frameworkBandLadder(frameworkVersionId),
            questionRepository.findPermanentlyExhaustedIds(topic.getId(), studentId)
        );
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
        return jsonSerializationPort.toStringField(question.getDifficultyFeaturesJson(), "reasoning_type");
    }
}
