package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.TopicSuggestion;

public interface TopicSuggestionRepository {

    Optional<TopicSuggestion> findById(UUID id);

    TopicSuggestion save(TopicSuggestion suggestion);

}
