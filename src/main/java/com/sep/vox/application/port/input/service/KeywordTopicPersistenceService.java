package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.TopicSuggestionRepository;

/**
 * Writes a topic newly generated from a keyword + writes the request log row -- TWO writes TIED TOGETHER by
 * business meaning, so they must succeed or be cancelled together: a topic that exists with no log row
 * pointing at it can later not be traced back to which keyword of whom created it.
 *
 * Since 2026-08-07 this log row NO LONGER governs anything (the 3-per-week quota was removed, AI search has no
 * limit on the number of times) -- but atomicity is kept, for the traceability reason above.
 *
 * A separate bean, NOT a method in TopicSuggestionService, because:
 * (a) self-invocation bypasses the AOP proxy so @Transactional would have no effect;
 * (b) generateFromKeyword makes a slow LLM call in the middle, which must not sit inside a transaction.
 */
@Service
@RequiredArgsConstructor
public class KeywordTopicPersistenceService {

    private final PracticeTopicRepository practiceTopicRepository;
    private final TopicSuggestionRepository topicSuggestionRepository;

    @Transactional
    public UUID createTopicAndRecordRequest(
            UUID studentId,
            String keyword,
            String topicName,
            String dimension,
            String curriculumGroup,
            String temporalAffordance) {
        Instant now = Instant.now();
        PracticeTopic saved = practiceTopicRepository.save(PracticeTopic.generatedFromName(
                "USER_GENERATED",
                topicName,
                TopicSuggestionService.normalize(topicName),
                dimension,
                curriculumGroup,
                temporalAffordance,
                now));
        topicSuggestionRepository.save(TopicSuggestion.keywordRequested(studentId, keyword, "CREATED", now));
        return saved.getId();
    }
}
