package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.personalization.TopicSuggestionRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.TopicSuggestionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicSuggestionRepository;

@Repository
public class TopicSuggestionRepositoryImpl implements TopicSuggestionRepository {

    private final SpringDataTopicSuggestionRepository repository;

    public TopicSuggestionRepositoryImpl(
            SpringDataTopicSuggestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TopicSuggestion> findById(UUID id) {
        return repository.findById(id).map(TopicSuggestionMapper::toDomain);
    }

    @Override
    public TopicSuggestion save(TopicSuggestion suggestion) {
        return TopicSuggestionMapper.toDomain(
            repository.save(TopicSuggestionMapper.toJpa(suggestion))
        );
    }

    @Override
    public int countWeeklyKeywordRequests(UUID studentId) {
        return repository.countWeeklyKeywordRequests(studentId);
    }

}
