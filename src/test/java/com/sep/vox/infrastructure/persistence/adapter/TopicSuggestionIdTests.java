package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.TopicSuggestionRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(TopicSuggestionRepositoryImpl.class)
class TopicSuggestionIdTests {

    @Autowired
    private TopicSuggestionRepository suggestionRepository;

    @Test
    void should_store_a_version_7_id_for_a_suggestion_saved_with_a_null_id() {
        TopicSuggestion saved = suggestionRepository.save(TopicSuggestion.builder()
            .studentId(UUID.randomUUID())
            .suggestedTopicName("Space travel")
            .interestDimension("SCIENCE")
            .confidence(new BigDecimal("0.800"))
            .reasonText("Likes astronomy")
            .status("PENDING")
            .createdAt(Instant.now())
            .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId().version()).isEqualTo(7);
        assertThat(suggestionRepository.findById(saved.getId())).isPresent();
    }
}
