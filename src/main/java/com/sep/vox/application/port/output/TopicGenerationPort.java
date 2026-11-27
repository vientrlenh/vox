package com.sep.vox.application.port.output;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calls the AI service (Python agents) to propose practice topics, index topics and find topics
 * close in meaning. Implemented by TopicGenerationClient.
 *
 * <p>Every method DEGRADES GRACEFULLY: a network error/timeout/malformed response returns an empty
 * list (or does nothing for {@link #index}) and is not thrown outward -- callers in the application
 * rely on this so they do not have to wrap calls in try/catch.
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

    /** Finds topics close in meaning to the keyword. Returns ids with their similarity, NOT names. */
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
