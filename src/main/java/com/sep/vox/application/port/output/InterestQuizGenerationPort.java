package com.sep.vox.application.port.output;

import java.util.List;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

/**
 * Generates situational interest-quiz questions with AI. Implemented by InterestQuizGenerationClient.
 *
 * <p>Degrades gracefully like TopicGenerationPort: when the AI service is down it returns an empty list instead of throwing.
 */
public interface InterestQuizGenerationPort {

    /**
     * @param dimensions the current catalog of interest dimensions. Sent along instead of being
     *                   hardcoded in Python, so a dimension added by SYSTEM_ADMIN takes effect immediately.
     */
    List<InterestQuizSeedItem> generate(
        int maxItems,
        List<String> existingStatements,
        List<String> dimensions);
}
