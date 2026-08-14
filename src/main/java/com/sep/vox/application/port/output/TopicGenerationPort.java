package com.sep.vox.application.port.output;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gọi dịch vụ AI (Python agents) để đề xuất chủ đề luyện, đánh chỉ mục chủ đề và tìm chủ đề gần
 * nghĩa. Implement bởi TopicGenerationClient.
 *
 * <p>Mọi method đều SUY GIẢM ÊM: lỗi mạng/timeout/response dị dạng trả về danh sách rỗng (hoặc
 * không làm gì với {@link #index}), không ném ra ngoài -- phía gọi ở application dựa vào tính chất
 * này để không phải bọc try/catch.
 */
public interface TopicGenerationPort {

    List<TopicProposal> propose(
        UUID studentId,
        List<KeywordEvidence> keywordEvidence,
        Map<String, Double> interestScores,
        List<String> rejectedTopics,
        List<String> exhaustedTopics,
        boolean searchKeyword,
        int maxProposals,
        List<String> dimensions);

    void index(
        String topicId,
        String name,
        String description,
        boolean active,
        UUID studentId,
        String status);

    /** Tìm chủ đề gần nghĩa với từ khoá. Trả về id kèm độ tương đồng, KHÔNG trả tên. */
    List<TopicSearchHit> searchByVector(String keyword, int limit);

    record TopicSearchHit(UUID topicId, double similarity) {
    }

    record KeywordEvidence(String keyword, int sessionCount) {
    }

    record TopicProposal(
        String name,
        String interestDimension,
        String curriculumGroup,
        String temporalAffordance,
        double confidence,
        String reasonText,
        String evidenceType,
        String evidenceJson) {
    }
}
