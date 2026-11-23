package com.sep.vox.application.port.input.service;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import java.math.BigDecimal;
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
import com.sep.vox.domain.service.personalization.TensePolicy;

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
    public TopicFromKeywordResult generateFromKeyword(UUID studentId, String keyword) {
        String normalized = normalize(keyword);
        if (normalized.isBlank()) {
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        TopicNameCard existingTopic = findNearExistingTopic(normalized);
        if (existingTopic != null) {
            recordKeywordRequest(studentId, keyword, "MATCHED_EXISTING");
            return new TopicFromKeywordResult(
                offerFor(
                    studentId, existingTopic.id(), existingTopic.name(),
                    existingTopic.interestDimension(), false, null, null
                ),
                "MATCHED_EXISTING"
            );
        }
        // REMOVED the quota of 3 requests/week (2026-08-07). The old condition was
        // `weeklyRequestCount(studentId) >= 3 || unsuitable(normalized)`.
        //
        // It counted EVERY row that has a keyword in the calendar week, without filtering by result -- but
        // recordKeywordRequest writes a row in all four branches, so the three branches that cost NO LLM call
        // (MATCHED_EXISTING, REJECTED_UNSUITABLE, OUT_OF_ASSESSMENT_SCOPE) were still charged against the quota. A student
        // who typed a keyword pointing exactly at an existing topic also lost one of the three requests.
        //
        // The real cost already has its proper place of control: the PRACTICE quota is computed by speaking seconds
        // (SubmitPracticeTurnUseCase -> ConsumeQuotaService). Blocking here as well tightens the same
        // pocket of money twice, by an unrelated unit.
        if (unsuitable(normalized)) {
            recordKeywordRequest(studentId, keyword, "REJECTED_UNSUITABLE");
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        String goal = currentGoal(studentId);
        if ("ASSESSMENT_PREP".equals(goal)) {
            // ASSESSMENT_PREP only practices on topics taken from the question bank (question_bank/
            // question_topic) of exactly the school + grade -- it does not let the AI generate new topics from
            // free keywords, and keeps the result code OUT_OF_ASSESSMENT_SCOPE (the meaning is still correct).
            recordKeywordRequest(studentId, keyword, "OUT_OF_ASSESSMENT_SCOPE");
            return new TopicFromKeywordResult(null, "OUT_OF_ASSESSMENT_SCOPE");
        }
        List<TopicGenerationPort.TopicProposal> proposals = topicGenerationPort.propose(
            studentId,
            List.of(new KeywordEvidence(keyword, 1)),
            interestScores(studentId),
            List.of(),   // xem ghi chu o cho go rejectedTopicNames
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
            recordKeywordRequest(studentId, keyword, "MATCHED_EXISTING");
            return new TopicFromKeywordResult(
                offerFor(
                    studentId, duplicateTopic.id(), duplicateTopic.name(),
                    duplicateTopic.interestDimension(), false, null, null
                ),
                "MATCHED_EXISTING"
            );
        }
        // Tạo topic + ghi dòng nhật ký yêu cầu phải atomic với nhau -- tách sang bean
        // riêng có @Transactional (xem KeywordTopicPersistenceService), KHÔNG bọc
        // transaction quanh cả hàm này vì generationClient.propose() ở trên là
        // cuộc gọi LLM chậm.
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
    // GỠ 2026-08-11: MAX_PROPOSALS_PER_RUN = 8. Số đề xuất xin LLM nay là `requestedCount` --
    // đúng số cần, do TopicOfferBackfillService tính từ kích thước kho (8 khi đang xây, 2 khi đã
    // ổn định). Xem chú thích tại lời gọi propose().

    public List<PracticeTopicOffer> synchronousOffers(UUID studentId, int requestedCount) {
        if (requestedCount <= 0 || "ASSESSMENT_PREP".equals(currentGoal(studentId))) {
            return List.of();
        }
        List<TopicGenerationPort.TopicProposal> proposals = topicGenerationPort.propose(
            studentId,
            List.of(),
            interestScores(studentId),
            List.of(),   // xem ghi chu o cho go rejectedTopicNames
            practiceTopicRepository.findExhaustedTopicNames(studentId),
            false,
            // Xin ĐÚNG số cần, không xin dư nữa.
            //
            // Bản trước cứng MAX_PROPOSALS_PER_RUN = 8 với lý do "bộ lọc trùng-gần cắt rất mạnh,
            // xin đúng số cần thì mỗi lượt chỉ ra được 1 chủ đề". Lý do đó đúng ở thời điểm ấy,
            // nhưng nó bù bằng cách trả tiền TRƯỚC cho phần có thể bị vứt: ở chế độ ổn định
            // (kho >= POOL_TARGET) requestedCount chỉ là 2, nên 6 đề xuất được sinh, được NHÚNG
            // ở TopicDedupeNode, rồi bỏ -- mỗi phiên.
            //
            // Nay Python có vòng đề xuất lại (MAX_PROPOSAL_ROUNDS): va chạm không còn bị vứt im
            // lặng mà quay lại thành prompt kèm đúng tên chủ đề vừa đâm vào. Nên chỉ trả thêm
            // KHI THẬT SỰ va chạm, thay vì trả trước cho mọi lượt.
            //
            // Vẫn phải <= MAX_TOPIC_PROPOSALS bên Python (schemas/topic_generation.py), không thì
            // 422 ngay ở cửa -- requestedCount lớn nhất là BACKFILL_COUNT_BUILDING = 8, vẫn nằm trong.
            requestedCount,
            dimensionCodes()
        );
        List<PracticePlanningResponses.PracticeTopicOffer> result = new ArrayList<PracticeTopicOffer>();
        // Nạp ứng viên MỘT lần cho cả vòng lặp. Bản cũ gọi findNearExistingTopic() ngay trong
        // vòng, mà hàm đó tự nạp toàn bộ bảng -- tức tối đa MAX_PROPOSALS_PER_RUN lượt nạp + tokenize
        // lại mọi tên chủ đề, mỗi lượt sinh.
        List<TopicNameCard> candidates = activeNameCards();
        // Sổ kế toán một lượt sinh -- xem log cuối hàm để biết vì sao cần.
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
            UUID topicId = createTopic(
                proposal.name(), proposal.interestDimension(), proposal.curriculumGroup(),
                "AI_SUGGESTED", proposal.temporalAffordance()
            );
            // BẮT BUỘC: nối chủ đề vừa tạo vào danh sách ứng viên. Bản cũ nạp lại DB mỗi vòng nên
            // tự nhiên thấy được cái vừa tạo; nâng ra ngoài mà quên bước này là mất khả năng chống
            // trùng TRONG CÙNG MỘT LƯỢT -- đúng tình huống chú thích ở lời gọi propose() mô tả:
            // LLM hay đề xuất cụm chủ đề gần nhau, nên cái thứ 2-3 phải được so với cái thứ 1 vừa
            // tạo xong.
            candidates.add(new TopicNameCard(
                topicId, proposal.name(), proposal.interestDimension()
            ));
            topicGenerationPort.index(
                topicId.toString(), proposal.name(), proposal.reasonText(), true, null, "ACTIVE"
            );
            result.add(offerFor(
                studentId, topicId, proposal.name(), proposal.interestDimension(), false,
                (int) Math.round(proposal.confidence() * 100), proposal.reasonText()
            ));
        }
        // Sổ kế toán một lượt sinh. Trước đây năng suất bằng 0 bị NUỐT IM LẶNG:
        // backfillAsync chỉ log số tạo được, còn "xin 8 mà 8 cái đều trùng" thì không phân biệt
        // được với "kho đã đủ nên không cần tạo". Chạy vài ngày với dòng này là trả lời được ba
        // câu: vòng phản hồi va chạm có đáng làm không, MAX_ROUNDS nên là mấy, và hạ
        // vòng đề xuất lại bên Python có đang cứu được va chạm không.
        //
        // `proposals.size()` đã là số CÒN LẠI sau khi TopicDedupeNode bên Python lọc bằng Chroma
        // (ngưỡng cosine 0.90), nên chênh lệch so với số xin chính là phần Python đã cắt.
        log.info(
            "[topic-gen] học sinh={} xin={} python_trả={} java_chặn_trùng={} vượt_hạn_mức={} tạo_được={}{}",
            studentId, requestedCount, proposals.size(), collidedWith.size(),
            skippedOverQuota, result.size(),
            collidedWith.isEmpty() ? "" : " | va chạm: " + String.join(" ; ", collidedWith)
        );
        return result;
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
        PracticeTopic saved = practiceTopicRepository.save(PracticeTopic.builder()
                .id(null)
                .name(name)
                .normalizedName(normalize(name))
                .description(name)
                .source(source)
                .interestDimension(dimension)
                .curriculumGroup(curriculumGroup == null ? "OUT_OF_CURRICULUM" : curriculumGroup)
                .active(true)
                .createdAt(Instant.now())
                .sourceQuestionTopicId(null)
                .temporalAffordance(temporalAffordance == null ? TensePolicy.AFFORDANCE_MIXED : temporalAffordance)
                .build());
        return saved.getId();
    }

    private void recordKeywordRequest(UUID studentId, String keyword, String outcome) {
        topicSuggestionRepository.save(TopicSuggestion.builder()
                .id(null)
                .studentId(studentId)
                .suggestedTopicName(keyword)
                .keyword(keyword)
                .interestDimension("UNKNOWN")
                .curriculumGroup(null)
                .confidence(BigDecimal.ZERO)
                .reasonText(outcome)
                .evidenceJson("{}")
                .status("REQUESTED")
                .createdAt(Instant.now())
                .respondedAt(null)
                .build());
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
