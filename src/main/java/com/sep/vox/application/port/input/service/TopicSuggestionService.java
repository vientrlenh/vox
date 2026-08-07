package com.sep.vox.application.port.input.service;

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

import org.springframework.stereotype.Service;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.personalization.InterestDimensionRepository;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.repository.personalization.TopicSuggestionRepository;
import com.sep.vox.domain.service.personalization.TensePolicy;
import com.sep.vox.infrastructure.service.TopicGenerationClient;
import com.sep.vox.infrastructure.service.TopicGenerationClient.KeywordEvidence;

/**
 * Đề xuất / tạo / khớp chủ đề luyện tập -- gộp từ TopicSuggestionRepositoryImpl vì đây là thuật
 * toán nghiệp vụ (khớp mờ, gọi LLM, chống trùng, lọc theo phạm vi trường) chạm nhiều aggregate
 * (TopicSuggestion, PracticeTopic, LearnerProfile), không phải việc của 1 adapter CRUD.
 */
@Service
public class TopicSuggestionService {

    private static final Set<String> UNSUITABLE = Set.of(
        "porn", "sex", "drugs", "weapon", "gambling", "hate", "suicide"
    );

    private final TopicSuggestionRepository topicSuggestionRepository;
    private final PracticeTopicRepository practiceTopicRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final TopicGenerationClient generationClient;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final KeywordTopicPersistenceService keywordTopicPersistenceService;
    private final InterestDimensionRepository interestDimensionRepository;

    public TopicSuggestionService(
            TopicSuggestionRepository topicSuggestionRepository,
            PracticeTopicRepository practiceTopicRepository,
            LearnerProfileRepository learnerProfileRepository,
            TopicGenerationClient generationClient,
            PracticeTopicOfferEnrichmentService enrichmentService,
            KeywordTopicPersistenceService keywordTopicPersistenceService,
            InterestDimensionRepository interestDimensionRepository) {
        this.interestDimensionRepository = interestDimensionRepository;
        this.topicSuggestionRepository = topicSuggestionRepository;
        this.practiceTopicRepository = practiceTopicRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.generationClient = generationClient;
        this.enrichmentService = enrichmentService;
        this.keywordTopicPersistenceService = keywordTopicPersistenceService;
    }

    // GỠ 2026-08-06: respond(studentId, suggestionId, accept) -- nhận/bỏ một gợi ý chủ đề.
    // Không còn nguồn nào tạo dòng PENDING sau khi bỏ đường suy chủ đề từ lời học sinh nói,
    // nên không còn gì để duyệt. Giao diện, GraphQL và use case đi kèm đã xoá cùng lúc.

    // Not @Transactional -- generationClient.propose() is a slow synchronous LLM call to the
    // Python agents service; see PracticeQuestionGenerationService.generateAndStore for the
    // same fix + rationale (HikariCP connection-pool starvation under load). Each repository
    // call here self-transacts via Spring Data's proxy.
    public TopicFromKeywordResult generateFromKeyword(UUID studentId, String keyword) {
        var normalized = normalize(keyword);
        if (normalized.isBlank()) {
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        var existingTopic = findNearExistingTopic(normalized);
        if (existingTopic != null) {
            recordKeywordRequest(studentId, keyword, "MATCHED_EXISTING");
            return new TopicFromKeywordResult(
                offerFor(
                    studentId, existingTopic.getId(), existingTopic.getName(),
                    existingTopic.getInterestDimension(), false, null, null
                ),
                "MATCHED_EXISTING"
            );
        }
        if (weeklyRequestCount(studentId) >= 3 || unsuitable(normalized)) {
            recordKeywordRequest(studentId, keyword, "REJECTED_UNSUITABLE");
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        var goal = currentGoal(studentId);
        if ("EXAM_PREP".equals(goal)) {
            // EXAM_PREP chỉ luyện trên topic lấy từ ngân hàng câu hỏi (question_bank/
            // question_topic) của đúng trường + khối -- không cho AI tự sinh topic mới từ
            // từ khoá tự do, giữ nguyên mã kết quả OUT_OF_EXAM_SCOPE (ý nghĩa vẫn đúng).
            recordKeywordRequest(studentId, keyword, "OUT_OF_EXAM_SCOPE");
            return new TopicFromKeywordResult(null, "OUT_OF_EXAM_SCOPE");
        }
        var proposals = generationClient.propose(
            studentId,
            List.of(new KeywordEvidence(keyword, 1)),
            interestScores(studentId),
            topicNames(),
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
        var proposal = proposals.get(0);
        var duplicateTopic = findNearExistingTopic(normalize(proposal.name()));
        if (duplicateTopic != null) {
            recordKeywordRequest(studentId, keyword, "MATCHED_EXISTING");
            return new TopicFromKeywordResult(
                offerFor(
                    studentId, duplicateTopic.getId(), duplicateTopic.getName(),
                    duplicateTopic.getInterestDimension(), false, null, null
                ),
                "MATCHED_EXISTING"
            );
        }
        // Tạo topic + ghi nhận lượt hạn mức phải atomic với nhau -- tách sang bean
        // riêng có @Transactional (xem KeywordTopicPersistenceService), KHÔNG bọc
        // transaction quanh cả hàm này vì generationClient.propose() ở trên là
        // cuộc gọi LLM chậm.
        var topicId = keywordTopicPersistenceService.createTopicAndRecordRequest(
            studentId,
            keyword,
            proposal.name(),
            proposal.interestDimension(),
            proposal.curriculumGroup(),
            proposal.temporalAffordance()
        );
        generationClient.index(
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
    // Bảng topic_suggestion VẪN CÒN, nhưng nay chỉ là nhật ký yêu cầu theo từ khoá
    // (recordKeywordRequest + hạn mức 3 lượt/tuần ở countWeeklyKeywordRequests).

    // Not @Transactional -- same reason as generateFromKeyword above. This one is the hottest
    // path (called synchronously from practiceTopicOffers whenever ranked offers are thin), so
    // it's the most load-bearing fix of the three.
    /** So de xuat xin LLM moi luot, truoc khi loc trung. Xem chu thich trong than ham. */
    private static final int MAX_PROPOSALS_PER_RUN = 8;

    public List<PracticeTopicOffer> synchronousOffers(UUID studentId, int requestedCount) {
        if (requestedCount <= 0 || "EXAM_PREP".equals(currentGoal(studentId))) {
            return List.of();
        }
        var proposals = generationClient.propose(
            studentId,
            List.of(),
            interestScores(studentId),
            topicNames(),
            List.of(),   // xem ghi chu o cho go rejectedTopicNames
            practiceTopicRepository.findExhaustedTopicNames(studentId),
            false,
            // Xin NHIEU hon so can: bo loc trung-gan ngay ben duoi cat rat manh -- do that
            // cho thay 3 de xuat chi song 1 (LLM hay de xuat cum chu de gan nhau, roi cai
            // thu 2-3 bi so voi cai thu 1 vua tao xong). Xin dung so can thi moi luot chi
            // ra duoc 1 chu de, kho lon rat cham. Phai <= MAX_TOPIC_PROPOSALS ben Python
            // (schemas/topic_generation.py), khong thi 422 ngay o cua.
            MAX_PROPOSALS_PER_RUN,
            dimensionCodes()
        );
        var result = new ArrayList<PracticeTopicOffer>();
        for (var proposal : proposals) {
            if (result.size() >= requestedCount
                    || findNearExistingTopic(normalize(proposal.name())) != null) {
                continue;
            }
            var topicId = createTopic(
                proposal.name(), proposal.interestDimension(), proposal.curriculumGroup(),
                "AI_SUGGESTED", proposal.temporalAffordance()
            );
            generationClient.index(
                topicId.toString(), proposal.name(), proposal.reasonText(), true, null, "ACTIVE"
            );
            result.add(offerFor(
                studentId, topicId, proposal.name(), proposal.interestDimension(), false,
                (int) Math.round(proposal.confidence() * 100), proposal.reasonText()
            ));
        }
        return result;
    }

    /** Danh mục chiều sở thích hiện hành, gửi xuống Python để ràng buộc đầu ra của LLM.
     * Gồm CẢ chiều hệ thống (ACADEMIC_EXAM) vì đây là gán nhãn cho chủ đề, không phải hỏi
     * học sinh -- khác với quiz, nơi chỉ dùng chiều quiz_eligible. */
    private List<String> dimensionCodes() {
        return interestDimensionRepository.findActive().stream()
            .map(InterestDimension::getCode)
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
        var decomposed = Normalizer.normalize(
            value.toLowerCase(Locale.ROOT).strip().replace('đ', 'd'),
            Normalizer.Form.NFD
        );
        return decomposed.replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .strip();
    }

    static double tokenSimilarity(String left, String right) {
        var leftTokens = new HashSet<>(List.of(normalize(left).split(" ")));
        var rightTokens = new HashSet<>(List.of(normalize(right).split(" ")));
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        var intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        var union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    private PracticeTopic findNearExistingTopic(String normalized) {
        return practiceTopicRepository.findAllActive().stream()
            .filter(topic -> normalize(topic.getName()).equals(normalized)
                || tokenSimilarity(topic.getName(), normalized) >= 0.90)
            .findFirst()
            .orElse(null);
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
        var saved = practiceTopicRepository.save(new PracticeTopic(
            null,
            name,
            normalize(name),
            name,
            source,
            dimension,
            curriculumGroup == null ? "OUT_OF_CURRICULUM" : curriculumGroup,
            true,
            Instant.now(),
            null,
            temporalAffordance == null ? TensePolicy.AFFORDANCE_MIXED : temporalAffordance
        ));
        return saved.getId();
    }

    private int weeklyRequestCount(UUID studentId) {
        return topicSuggestionRepository.countWeeklyKeywordRequests(studentId);
    }

    private void recordKeywordRequest(UUID studentId, String keyword, String outcome) {
        topicSuggestionRepository.save(new TopicSuggestion(
            null,
            studentId,
            keyword,
            keyword,
            "UNKNOWN",
            null,
            BigDecimal.ZERO,
            outcome,
            "{}",
            "REQUESTED",
            Instant.now(),
            null
        ));
    }

    private List<String> topicNames() {
        return practiceTopicRepository.findAllActiveOrderByName().stream()
            .map(PracticeTopic::getName)
            .toList();
    }

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
