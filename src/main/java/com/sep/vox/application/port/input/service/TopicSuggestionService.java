package com.sep.vox.application.port.input.service;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.TopicGenerationPort;
import com.sep.vox.application.port.output.TopicGenerationPort.KeywordEvidence;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.InterestDimensionRepository;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.TopicSuggestionRepository;

/**
 * Đề xuất / tạo / khớp chủ đề luyện tập -- gộp từ TopicSuggestionRepositoryImpl vì đây là thuật
 * toán nghiệp vụ (khớp mờ, gọi LLM, chống trùng, lọc theo phạm vi trường) chạm nhiều aggregate
 * (TopicSuggestion, PracticeTopic, LearnerProfile), không phải việc của 1 adapter CRUD.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicSuggestionService {

    private static final Set<String> UNSUITABLE = Set.of(
        "porn", "sex", "drugs", "weapon", "gambling", "hate", "suicide"
    );

    private final TopicSuggestionRepository topicSuggestionRepository;
    private final PracticeTopicRepository practiceTopicRepository;
    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final TopicGenerationPort topicGenerationPort;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final KeywordTopicPersistenceService keywordTopicPersistenceService;
    private final InterestDimensionRepository interestDimensionRepository;

    // GỠ 2026-08-06: respond(studentId, suggestionId, accept) -- nhận/bỏ một gợi ý chủ đề.
    // Không còn nguồn nào tạo dòng PENDING sau khi bỏ đường suy chủ đề từ lời học sinh nói,
    // nên không còn gì để duyệt. Giao diện, GraphQL và use case đi kèm đã xoá cùng lúc.

    // Not @Transactional -- generationClient.propose() is a slow synchronous LLM call to the
    // Python agents service; see PracticeQuestionGenerationService.generateAndStore for the
    // same fix + rationale (HikariCP connection-pool starvation under load). Each repository
    // call here self-transacts via Spring Data's proxy.
    /**
     * There is NO quota of 3 requests/week here any more (removed 2026-08-07). The old condition was
     * `weeklyRequestCount(studentId) >= 3 || unsuitable(normalized)`.
     *
     * <p>It counted EVERY row that has a keyword in the calendar week, without filtering by result -- but
     * recordKeywordRequest writes a row in all four branches, so the three branches that cost NO LLM call
     * (MATCHED_EXISTING, REJECTED_UNSUITABLE, OUT_OF_ASSESSMENT_SCOPE) were still charged against the quota. A student
     * who typed a keyword pointing exactly at an existing topic also lost one of the three requests.
     *
     * <p>The real cost already has its proper place of control: the PRACTICE quota is computed by speaking seconds
     * (SubmitPracticeTurnUseCase -> ConsumeQuotaService). Blocking here as well tightens the same
     * pocket of money twice, by an unrelated unit.
     */
    public TopicFromKeywordResult generateFromKeyword(UUID studentId, String keyword) {
        String normalized = normalize(keyword);
        if (normalized.isBlank()) {
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        TopicNameCard existingTopic = findNearExistingTopic(normalized);
        if (existingTopic != null) {
            return matchedExisting(studentId, keyword, existingTopic);
        }
        if (unsuitable(normalized)) {
            recordKeywordRequest(studentId, keyword, "REJECTED_UNSUITABLE");
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        if ("ASSESSMENT_PREP".equals(currentGoal(studentId))) {
            // ASSESSMENT_PREP only practices on topics taken from the question bank (question_bank/
            // question_topic) of exactly the school + grade -- it does not let the AI generate new topics from
            // free keywords, and keeps the result code OUT_OF_ASSESSMENT_SCOPE (the meaning is still correct).
            recordKeywordRequest(studentId, keyword, "OUT_OF_ASSESSMENT_SCOPE");
            return new TopicFromKeywordResult(null, "OUT_OF_ASSESSMENT_SCOPE");
        }
        return proposeAndCreateTopic(studentId, keyword);
    }

    private TopicFromKeywordResult matchedExisting(UUID studentId, String keyword, TopicNameCard topic) {
        recordKeywordRequest(studentId, keyword, "MATCHED_EXISTING");
        return new TopicFromKeywordResult(
            offerFor(
                studentId, topic.id(), topic.name(),
                topic.interestDimension(), false, null, null
            ),
            "MATCHED_EXISTING"
        );
    }

    private TopicFromKeywordResult proposeAndCreateTopic(UUID studentId, String keyword) {
        List<TopicGenerationPort.TopicProposal> proposals = topicGenerationPort.propose(
            studentId,
            List.of(new KeywordEvidence(keyword, 1)),
            interestScores(studentId),
            List.of(),   // see the note at the removed rejectedTopicNames
            practiceTopicRepository.findExhaustedTopicNames(studentId),
            true,
            1,
            dimensionCodes()
        );
        if (proposals.isEmpty()) {
            recordKeywordRequest(studentId, keyword, "REJECTED_UNSUITABLE");
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        TopicGenerationPort.TopicProposal proposal = proposals.get(0);
        TopicNameCard duplicateTopic = findNearExistingTopic(normalize(proposal.name()));
        if (duplicateTopic != null) {
            return matchedExisting(studentId, keyword, duplicateTopic);
        }
        return createTopicFromProposal(studentId, keyword, proposal);
    }

    /**
     * Creating the topic + writing the request-log row must be atomic together -- split into a separate bean
     * with @Transactional (see KeywordTopicPersistenceService), and NO transaction wraps the whole of
     * generateFromKeyword because generationClient.propose() above is a slow LLM call.
     */
    private TopicFromKeywordResult createTopicFromProposal(
            UUID studentId, String keyword, TopicGenerationPort.TopicProposal proposal) {
        UUID topicId = keywordTopicPersistenceService.createTopicAndRecordRequest(
            studentId,
            keyword,
            proposal.name(),
            proposal.interestDimension(),
            proposal.curriculumGroup(),
            proposal.temporalAffordance()
        );
        topicGenerationPort.index(
            topicId.toString(), proposal.name(), proposal.reasonText(), true, null, "ACTIVE"
        );
        return new TopicFromKeywordResult(
            offerFor(
                studentId, topicId, proposal.name(), proposal.interestDimension(), false,
                (int) Math.round(proposal.confidence() * 100), proposal.reasonText()
            ),
            "CREATED"
        );
    }

    // GỠ 2026-08-06 -- refreshSuggestions / studentsDueForSuggestionRefresh.
    //
    // Đường cũ: đọc transcript 30 ngày gần nhất, đếm từ nội dung theo số buổi có nhắc, lấy 30 từ
    // đứng đầu làm bằng chứng rồi nhờ LLM đề xuất chủ đề mới -- "AI nghe thấy em hay nhắc tới X".
    // Bỏ theo yêu cầu: hệ thống không suy diễn chủ đề từ lời học sinh nói nữa.
    //
    // Hai đường sinh chủ đề CÒN LẠI đều không đọc transcript, nên không đụng tới:
    //   - generateFromKeyword  : học sinh tự gõ từ khoá, bằng chứng là chính từ khoá đó
    //   - synchronousOffers    : truyền evidence rỗng, chỉ dựa điểm sở thích
    //
    // Cả luồng duyệt gợi ý đã xoá theo (pendingSuggestions / respond / GraphQL / thẻ bên Flutter):
    // không còn nguồn tạo dòng PENDING thì không còn gì để duyệt.
    //
    // Bảng topic_suggestions VẪN CÒN, nhưng nay chỉ là nhật ký yêu cầu theo từ khoá
    // (recordKeywordRequest) -- ghi để xem lại học sinh đã tìm gì, không chi phối gì cả.

    // Not @Transactional -- same reason as generateFromKeyword above. This one is the hottest
    // path (called synchronously from practiceTopicOffers whenever ranked offers are thin), so
    // it's the most load-bearing fix of the three.
    // REMOVED 2026-08-11: MAX_PROPOSALS_PER_RUN = 8. The number of proposals requested from the LLM is now
    // `requestedCount` -- exactly the number needed, computed by TopicOfferBackfillService from the bank size
    // (8 while building, 2 when steady). See the comment at the propose() call.
    /**
     * The candidates are loaded ONCE for the whole loop. The old version called findNearExistingTopic() inside the
     * loop, and that function loads the whole table itself -- i.e. up to MAX_PROPOSALS_PER_RUN loads + re-tokenizing
     * every topic name, on every generation run.
     */
    public List<PracticeTopicOffer> synchronousOffers(UUID studentId, int requestedCount) {
        if (requestedCount <= 0 || "ASSESSMENT_PREP".equals(currentGoal(studentId))) {
            return List.of();
        }
        List<TopicGenerationPort.TopicProposal> proposals = proposeFromInterests(studentId, requestedCount);
        List<PracticePlanningResponses.PracticeTopicOffer> result = new ArrayList<PracticeTopicOffer>();
        List<TopicNameCard> candidates = activeNameCards();
        List<String> collidedWith = new ArrayList<String>();
        int skippedOverQuota = 0;
        for (TopicGenerationPort.TopicProposal proposal : proposals) {
            if (result.size() >= requestedCount) {
                skippedOverQuota++;
                continue;
            }
            TopicNameCard duplicate = findNearExistingTopic(normalize(proposal.name()), candidates);
            if (duplicate != null) {
                collidedWith.add(proposal.name() + " ~ " + duplicate.name());
                continue;
            }
            result.add(createOffer(studentId, proposal, candidates));
        }
        logGenerationRun(studentId, requestedCount, proposals.size(), collidedWith, skippedOverQuota, result.size());
        return result;
    }

    /**
     * Ask for EXACTLY the number needed, no more.
     *
     * <p>The previous version hard-coded MAX_PROPOSALS_PER_RUN = 8 with the reason "the near-duplicate filter cuts very
     * hard, asking for exactly the number needed yields only 1 topic per run". That reason was right at the time,
     * but it compensated by paying UP FRONT for the part that may be thrown away: in steady mode
     * (bank >= POOL_TARGET) requestedCount is only 2, so 6 proposals were generated, EMBEDDED
     * in TopicDedupeNode, then dropped -- every session.
     *
     * <p>Now Python has a re-proposal loop (MAX_PROPOSAL_ROUNDS): a collision is no longer thrown away silently but
     * comes back as a prompt carrying exactly the topic name it just collided with. So we only pay extra
     * WHEN a collision really happens, instead of paying up front on every run.
     *
     * <p>It must still be <= MAX_TOPIC_PROPOSALS on the Python side (schemas/topic_generation.py), otherwise
     * a 422 right at the door -- the largest requestedCount is BACKFILL_COUNT_BUILDING = 8, still within it.
     */
    private List<TopicGenerationPort.TopicProposal> proposeFromInterests(UUID studentId, int requestedCount) {
        return topicGenerationPort.propose(
            studentId,
            List.of(),
            interestScores(studentId),
            List.of(),   // see the note at the removed rejectedTopicNames
            practiceTopicRepository.findExhaustedTopicNames(studentId),
            false,
            requestedCount,
            dimensionCodes()
        );
    }

    /**
     * MANDATORY: append the just-created topic to the candidate list. The old version reloaded the DB on every iteration so it
     * naturally saw what had just been created; hoisting the load out and forgetting this step loses the duplicate guard WITHIN
     * THE SAME RUN -- exactly the situation described at the propose() call: the LLM likes to propose clusters of nearby
     * topics, so the 2nd-3rd one must be compared against the 1st one that was just created.
     */
    private PracticeTopicOffer createOffer(
            UUID studentId, TopicGenerationPort.TopicProposal proposal, List<TopicNameCard> candidates) {
        UUID topicId = createTopic(
            proposal.name(), proposal.interestDimension(), proposal.curriculumGroup(),
            "AI_SUGGESTED", proposal.temporalAffordance()
        );
        candidates.add(new TopicNameCard(
            topicId, proposal.name(), proposal.interestDimension()
        ));
        topicGenerationPort.index(
            topicId.toString(), proposal.name(), proposal.reasonText(), true, null, "ACTIVE"
        );
        return offerFor(
            studentId, topicId, proposal.name(), proposal.interestDimension(), false,
            (int) Math.round(proposal.confidence() * 100), proposal.reasonText()
        );
    }

    /**
     * Accounting for one generation run. Before this, zero productivity was SWALLOWED SILENTLY:
     * backfillAsync only logs the number created, and "asked for 8 and all 8 were duplicates" cannot be told
     * apart from "the bank is full so nothing needed creating". Running a few days with this line answers three
     * questions: is the collision feedback loop worth doing, what MAX_ROUNDS should be, and whether lowering the
     * re-proposal rounds on the Python side is actually rescuing collisions.
     *
     * <p>`proposalCount` is already the number LEFT after TopicDedupeNode on the Python side filtered with Chroma
     * (cosine threshold 0.90), so the difference from the number requested is exactly the part Python cut.
     */
    private void logGenerationRun(
            UUID studentId, int requestedCount, int proposalCount, List<String> collidedWith,
            int skippedOverQuota, int createdCount) {
        log.info(
            "[topic-gen] học sinh={} xin={} python_trả={} java_chặn_trùng={} vượt_hạn_mức={} tạo_được={}{}",
            studentId, requestedCount, proposalCount, collidedWith.size(),
            skippedOverQuota, createdCount,
            collidedWith.isEmpty() ? "" : " | va chạm: " + String.join(" ; ", collidedWith)
        );
    }

    /**
     * The catalog of current interest dimensions, sent down to Python to constrain the LLM output.
     * It includes the system dimension (ACADEMIC_ASSESSMENT) as well because this labels a topic, it is not a question to
     * the student -- unlike the quiz, which only uses the quiz_eligible dimensions.
     */
    private List<String> dimensionCodes() {
        return interestDimensionRepository.findActive().stream()
            .map(dimension -> dimension.getCode())
            .toList();
    }

    // KHÔNG thêm lại confidenceForSessionCount ở đây. Trần độ tự tin theo lượng bằng chứng
    // (≤1 buổi → 0,5; 2 → 0,7; ≥3 → 0,85) sống ở PHÍA PYTHON, trong
    // TopicProposalNode/topic_proposal_node_config.py -- nơi nó được áp cùng lúc với các trần
    // khác (ungrounded 0,4; INTEREST 0,6; EXHAUSTED 0,7; SEARCH 0,95). Bản Java cũ ở đây là
    // bản sao không nơi nào gọi, và hai bản sẽ trôi lệch ngay lần sửa đầu tiên.

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(
            value.toLowerCase(Locale.ROOT).strip().replace('đ', 'd'),
            Normalizer.Form.NFD
        );
        return decomposed.replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .strip();
    }

    static double tokenSimilarity(String left, String right) {
        Set<String> leftTokens = new HashSet<>(List.of(normalize(left).split(" ")));
        Set<String> rightTokens = new HashSet<>(List.of(normalize(right).split(" ")));
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    /** Nạp ứng viên rồi so -- cho hai chỗ gọi ĐƠN LẺ (không nằm trong vòng lặp). */
    private TopicNameCard findNearExistingTopic(String normalized) {
        return findNearExistingTopic(normalized, activeNameCards());
    }

    /**
     * Bản thuần: so với danh sách ứng viên truyền vào, KHÔNG chạm DB.
     *
     * <p>Tách ra để {@code synchronousOffers} nạp ứng viên MỘT lần trước vòng lặp thay vì mỗi
     * vòng một lần. Bản cũ gọi trực tiếp trong vòng lặp qua tối đa {@code MAX_PROPOSALS_PER_RUN}
     * đề xuất, nên mỗi lượt sinh nạp lại toàn bộ bảng 8 lần và tokenize lại mọi tên chủ đề.
     */
    private TopicNameCard findNearExistingTopic(String normalized, List<TopicNameCard> candidates) {
        return candidates.stream()
            .filter(topic -> normalize(topic.name()).equals(normalized)
                || tokenSimilarity(topic.name(), normalized) >= 0.90)
            .findFirst()
            .orElse(null);
    }

    private List<TopicNameCard> activeNameCards() {
        return practiceTopicQueryRepository.findActiveNameCards().stream()
            .map(card -> new TopicNameCard(card.getId(), card.getName(), card.getInterestDimension()))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** Bản ghi rời để {@code synchronousOffers} nối thêm chủ đề VỪA tạo vào danh sách ứng viên. */
    private record TopicNameCard(UUID id, String name, String interestDimension) {
    }

    private PracticeTopicOffer offerFor(
            UUID studentId, UUID topicId, String name, String dimension, boolean savedByMe,
            Integer matchPercent, String rationale) {
        return new PracticeTopicOffer(
            topicId,
            name,
            dimension,
            savedByMe,
            matchPercent,
            enrichmentService.minutesForStudent(studentId),
            rationale,
            rationale == null ? List.of() : List.of(rationale)
        );
    }


    private UUID createTopic(
            String name,
            String dimension,
            String curriculumGroup,
            String source,
            String temporalAffordance) {
        PracticeTopic saved = practiceTopicRepository.save(PracticeTopic.generatedFromName(
                source,
                name,
                normalize(name),
                dimension,
                curriculumGroup,
                temporalAffordance,
                Instant.now()));
        return saved.getId();
    }

    private void recordKeywordRequest(UUID studentId, String keyword, String outcome) {
        topicSuggestionRepository.save(TopicSuggestion.keywordRequested(studentId, keyword, outcome, Instant.now()));
    }

    // GỠ 2026-08-11: topicNames(). Nó nạp toàn bộ chủ đề đang hoạt động chỉ để lấy tên, rồi gửi
    // cả danh sách xuống prompt. Xem chú thích tại lời gọi propose() để biết vì sao bỏ.

    // GỠ 2026-08-06: rejectedTopicNames + nearRejected. Cả hai đọc dòng status='REJECTED', mà
    // nơi duy nhất ghi trạng thái đó là respond() -- đã xoá. Giữ lại thì mỗi lượt gọi LLM phải
    // chạy thêm hai truy vấn chắc chắn rỗng. Tham số tương ứng của propose() vẫn giữ nguyên
    // (truyền List.of()) để không phải đổi hợp đồng với Python.

    private Map<String, Double> interestScores(UUID studentId) {
        return practiceTopicRepository.findInterestScoresByDimension(studentId);
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getGoalType() == null ? "ABILITY_IMPROVEMENT" : profile.getGoalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    private boolean unsuitable(String normalized) {
        return UNSUITABLE.stream().anyMatch(normalized::contains);
    }

}
